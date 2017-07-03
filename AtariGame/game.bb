Const WIN_W% = 192, WIN_H% = 160

Const R_WIN_W% = 640, R_WIN_H% = 480

AppTitle("Corpse Demolition")

Graphics WIN_W, WIN_H, 8, 2

SeedRnd MilliSecs()

Global frametimer=CreateTimer(60)
Global starttime=MilliSecs(),elapsedtime,fpscounter,curfps

Function collision(x, y, w, h, x2, y2, w2, h2)
	If y >= y2 + h2 Then Return False 
	If x >= x2 + w2 Then Return False 
	If y + h <= y2 Then Return False
	If x + w <= x2 Then Return False   
	Return True 
End Function 

Function distanceTo#(x, y, x2, y2)
	Return Sqr((x-x2)^2 + (y-y2)^2)
End Function 

Function frame%(cell%, size%) 
	Return cell * size + 1 + cell
End Function

Function drawLine(sx%, sy%, ex%, ey%, w%, interval%)
	Local a# = ATan2(ey-sy, ex-sx)
	Local d = distanceTo(sx, sy, ex, ey)
	
	For i = 0 To (d/w)-1 
		If w = 1 Then 
		 	If interval <> 0 Then 
				If i Mod interval = 0 Then Plot sx + Cos(a)*i, sy + Sin(a)*i
			Else
				Plot sx + Cos(a)*i, sy + Sin(a)*i
			End If
		Else
			If interval <> 0 Then 
				If i Mod interval = 0 Then Rect sx + Cos(a)*(i*w), sy + Sin(a)*(i*w), w, w
			Else
				Rect sx + Cos(a)*(i*w), sy + Sin(a)*(i*w), w, w
			End If
		End If
	Next
End Function

Function drawNumber(x%, y%, n$)
	Local l% = Len(n)
	
	For i% = 0 To l-1
		Local s% = Int(Mid(n, i+1, 1))
		DrawImageRect(spritesheet, x+i*8, y, frame(s, 8), 37, 8, 8)
	Next
End Function

Global reset
Global startscreen = 1

Global shootSound = LoadSound("sounds/shoot.wav")
Global deadSound = LoadSound("sounds/dead.wav")
Global breakSound = LoadSound("sounds/break.wav")

Global spritesheet = LoadImage("spritesheet.bmp")
MaskImage(spritesheet, 255, 0, 255)

Const RED% = 0
Const GREEN% = 1
Const BLUE% = 2
Const PURPLE% = 3

Type tile
	Field x%
	Field y%
		
	Field r%, g%, b%
	Field colorType%
	Field width%, height%
	
	Field broken
	Field dissolve%
	
	Field destroy
End Type

Function addTile(x2%, y2%, colorType2%)
	t.tile = New tile
	t\x = x2
	t\y = y2
	
	t\width = 16
	t\height = 8
	
	t\colorType = colorType2
	
	If t\colorType = RED Then 
		t\r = 180
		t\g = 88
		t\b = 108
	ElseIf t\colorType = GREEN Then
		t\r = 88
		t\g = 176
		t\b = 108
	ElseIf t\colorType = BLUE Then 
		t\r = 80
		t\g = 140
		t\b = 180
	Else
		t\r = 128
		t\g = 88
		t\b = 188
	End If  
End Function 

Function updateTile()
	For t.tile = Each tile
		If t\broken Then
			t\r = 255
			t\g = 255
			t\b = 255
			
			t\dissolve = t\dissolve + 1
			
			If t\dissolve Mod 8 = 0 Then 
				t\width = t\width - 2
				t\x = t\x + 1
			End If
		End If
	
		If t\destroy Then Delete t
	Next
End Function

Function drawTile()
	For t.tile = Each tile
		Color t\r, t\g, t\b
		Rect t\x, t\y, t\width, t\height
	Next
End Function 

Function collidedWithTile(x, y, w) 
	For t.tile = Each tile
		If collision(t\x, t\y, 16, 8, x, y, w, w) And t\broken = 0 Then
			Return 1
		End If
	Next

	Return 0
End Function

Type player
	Field x%
	Field y%
	
	Field speed%
	Field fireRate%
	Field shootAngle#
	
	Field direction%
	
	Field imx%
	Field imy%
	Field currentFrame%
	Field animationCount%
	Field size%
	
	Field score%
	
	Field dead
	
	Field destroy
End Type

Function addPlayer()
	p.player = New player
	p\x = WIN_W/2-4
	p\y = WIN_H/2-4
	
	p\imx = 1
	p\imy = 1
	
	p\size = 8
	
	p\speed = 1
End Function 

Global controller = 0
Global startDelay = 0

Function updatePlayer()
	For p.player = Each player
		If p\dead Then 
			p\speed = 0
			p\currentFrame = 4
			p\imx = frame(p\currentFrame, 8)
			spawnCount = 0
			If pressed() Then
				startDelay = 0 
				startscreen = 1
				reset = 1
			End If 
		End If 
	
		If KeyDown(203) Or JoyX(0) <= -0.4 Then
			If collidedWithTile(p\x - p\speed, p\y, p\size) = 0 And p\x-p\speed > 0 Then p\x = p\x - p\speed
			p\animationCount = p\animationCount + 1
		End If
		
		If KeyDown(205) Or JoyX(0) >= 0.4 Then
			If collidedWithTile(p\x + p\speed, p\y, p\size) = 0 And p\x-p\speed < WIN_W - 16 Then p\x = p\x + p\speed
			p\animationCount = p\animationCount + 1
		End If 
	
		If KeyDown(200) Or JoyY(0) <= -0.4 Then
			If collidedWithTile(p\x, p\y - p\speed, p\size) = 0 And p\y-p\speed > 0 Then p\y = p\y - p\speed
			p\animationCount = p\animationCount + 1
		End If 
		
		If KeyDown(208) Or JoyY(0) >= 0.4 Then
			If collidedWithTile(p\x, p\y + p\speed, p\size) = 0 And p\y-p\speed < WIN_H - 16 Then p\y = p\y + p\speed
			p\animationCount = p\animationCount + 1
		End If 
		
		If p\animationCount >= 8 Then
			p\currentFrame = p\currentFrame + 1
			If p\currentFrame >= 4 Then p\currentFrame = 0
			p\animationCount = 0 
		End If
		
		If p\dead = 0 Then p\imx = frame(p\currentFrame, p\size)
		
		If p\fireRate >= 1 Then
			p\fireRate = p\fireRate + 1
			If p\fireRate >= 32 Then p\fireRate = 0
		End If
		
		If startDelay < 16 Then startDelay = startDelay + 1
		
		If controller = 0 Then 
			If KeyDown(57) And startDelay >= 16 And p\fireRate = 0 Then 
				addProjectile(p\x + 2, p\y + 2, p\direction * -45, p\speed + 2, 1, 164, 112, 200, 4)
				PlaySound(shootSound)
				p\fireRate = 1
			End If
		
			If KeyDown(203) Then
				p\direction = 4
				
				If KeyDown(200) Then 
					p\direction = 3
				End If
				
				If KeyDown(208) Then 
					p\direction = 5
				End If 
			ElseIf KeyDown(205) Then
				p\direction = 0
				
				If KeyDown(200) Then 
					p\direction = 1
				End If
				
				If KeyDown(208) Then 
					p\direction = 7
				End If
			ElseIf KeyDown(200) Then
				p\direction = 2
			ElseIf KeyDown(208)
				p\direction = 6
			End If
			
			If KeyDown(200) = 0 And KeyDown(208) = 0 And KeyDown(203) = 0 And KeyDown(205) = 0 Then p\currentFrame = 0
		Else
			If JoyX(0) >= 0.5 Or JoyX(0) <= -0.5 Or JoyY(0) >= 0.5 Or JoyY(0) <= -0.5 Then
				p\shootAngle = ATan2(JoyY(), JoyX())
			End If
		
			If JoyX() > -0.4 And JoyX() < 0.4 And JoyY() > -0.4 And JoyY() < 0.4 Then 
				p\currentFrame = 0
				p\animationCount = 0
			End If
			
			If JoyDown(1) And startDelay >= 16 And p\fireRate = 0 Then 				
				addProjectile(p\x + 2, p\y + 2, p\shootAngle, p\speed + 2, 1, 164, 112, 200, 4)
				PlaySound(shootSound)
				p\fireRate = 1
			End If
		End If
	Next
End Function

Function raiseScore()
	For p.player = Each player
		p\score = p\score + 1
	Next
End Function

Function drawPlayer()
	For p.player = Each player
		DrawImageRect(spritesheet, p\x, p\y, p\imx, p\imy, p\size, p\size)
	Next
End Function

Function drawPlayerUI() 
	For p.player = Each player
		Local t$ = "SCORE: " + p\score
		If p\dead = 0 Then 
			For i = 0 To 7 
				DrawImageRect(spritesheet, WIN_W/2 - ((8*7)/2) + i*8, 16, frame(i, 8), 28, 8, 8) 
				drawNumber(WIN_W/2 - ((8*7)/2) + 6*8, 16, Str(p\score))
			Next
		Else
			;Color 136, 32, 32
			;Text WIN_W/2 - StringWidth("GAME OVER")/2, WIN_H/2 - StringHeight("GAME OVER"), "GAME OVER!"
			For i = 0 To 8 
				DrawImageRect(spritesheet, WIN_W/2 - ((8*7)/2) + i*8, WIN_H/2-4, frame(i, 8), 46, 8, 8) 
			Next
			
			For i = 0 To 7 
				DrawImageRect(spritesheet, WIN_W/2 - ((8*7)/2) + i*8, WIN_H/2 + 16, frame(i, 8), 28, 8, 8) 
				drawNumber(WIN_W/2 - ((8*7)/2) + 6*8, WIN_H/2 + 16, Str(p\score))
			Next
			
			For i = 0 To 13
				DrawImageRect(spritesheet, WIN_W/2 - (8*13)/2 + i*8, WIN_H/2+16*2, frame(i, 8), 55, 8, 8)
			Next
		End If
	Next
End Function

Type projectile
	Field x%
	Field y%
	
	Field velX#
	Field velY#
	
	Field player
	
	Field r%, g%, b%
	Field size%
	
	Field destroy
End Type

Function addProjectile(x2%, y2%, angle2#, speed2#, player2, r2%, g2%, b2%, size2%)
	p.projectile = New projectile
	p\x = x2
	p\y = y2
		
	p\velX = Cos(angle2) * speed2
	p\velY = Sin(angle2) * speed2
	
	p\player = player2
	
	p\r = r2
	p\g = g2
	p\b = b2
	
	p\size = size2
End Function

Function updateProjectile()
	For p.projectile = Each projectile
		p\x = p\x + p\velX
		p\y = p\y + p\velY
		
		If collidedWithTile(p\x, p\y, p\size) Then p\destroy = 1
		
		If p\destroy Then Delete p
	Next
End Function

Function drawProjectile()
	For p.projectile = Each projectile
		Color p\r, p\g, p\b
		Rect p\x, p\y, p\size, p\size
	Next
End Function

Type enemy
	Field x#
	Field y#
	
	Field moveAngle#
	Field speed#
	
	Field fireRate%
	
	Field health%
	Field dead
	Field pickedUp
	Field sentAway
	Field realsedKey
	Field lifeTime%
	
	Field colorType%
	Field r%, g%, b%
	Field size%
	
	Field destroy
End Type

Function addEnemy(x2%, y2%, colorType2%)
	e.enemy = New enemy
	e\x = x2
	e\y = y2
	
	e\health = 1
	
	e\size = 8
	
	e\speed = 0.5+Rnd(-0.1, 0)
	
	e\colorType = colorType2
	
	e\realsedKey = 1
	
	If e\colorType = RED Then 
		e\r = 180
		e\g = 88
		e\b = 108
	ElseIf e\colorType = GREEN Then
		e\r = 88
		e\g = 176
		e\b = 108
	ElseIf e\colorType = BLUE Then 
		e\r = 80
		e\g = 140
		e\b = 180
	Else
		e\r = 128
		e\g = 88
		e\b = 188
	End If  
End Function

Function pressed()
	If controller Then 
		Return JoyDown(1)
	Else
		Return KeyDown(57)
	End If 
End Function

Function updateEnemy()
	For e.enemy = Each enemy
		If e\dead = 0 Then
			For pl.player = Each player
				e\moveAngle = ATan2(pl\y-e\y, pl\x-e\x)
				
				If collision(pl\x, pl\y, pl\size, pl\size, e\x, e\y, 8, 8) Then 
					pl\dead = 1
					PlaySound(deadSound)
					e\destroy = 1
				End If 
			Next
		 
			For p.projectile = Each projectile
				If p\player And collision(p\x, p\y, p\size, p\size, e\x, e\y, 8, 8) Then
					e\health = e\health - 1
					If e\health <= 0 Then PlaySound(deadSound)
					p\destroy = 1
				End If
			Next
		Else
			For pl.player = Each player
				If e\pickedUp Then
					If KeyDown(203) Or JoyX(0) <= -0.4 Then e\moveAngle = e\moveAngle - 4
					If KeyDown(205) Or JoyX(0) >= 0.4 Then e\moveANgle = e\moveAngle + 4
					
					pl\fireRate = 1
					pl\currentFrame = 0
					
					If pressed() And e\realsedKey Then
						e\speed = 2
						pl\speed = 1
						e\sentAway = 1
						
						e\pickedUp = 0
					End If
				End If
				
				If collision(pl\x, pl\y, pl\size, pl\size, e\x, e\y, 8, 8) And pressed() And e\pickedUp = 0 And e\sentAway = 0 And e\lifeTime <= 256+128 Then
					startDelay = 0
					e\pickedUp = 1
					pl\speed = 0
					e\realsedKey = 0
				End If
			Next 
		End If
		
		If e\realsedKey = 0 Then
			If KeyDown(57) = 0 And JoyDown(1) = 0 Then e\realsedKey = 1
		End If
		
		If e\lifeTime >= 256+128 Then 
			If e\lifeTime Mod 8 = 0 Then 
				e\size = e\size - 2
				e\x = e\x + 1
				e\y = e\y + 1
			End If
		End If
		
		If e\health <= 0 Then 
			e\dead = 1
			
			If e\pickedUp = 0 And e\sentAway = 0 Then 
				e\lifeTime = e\lifeTime + 1
			End If
			If e\sentAway = 0 Then 
				e\speed = 0
			End If
		End If
		
		If e\sentAway Then
			For t.tile = Each tile
				If collision(t\x, t\y, 16, 8, e\x, e\y, 8, 8) Then
					If e\colorType = t\colorType Then 
						raiseScore()
						PlaySound(breakSound)
						t\broken = 1
					End If
					e\destroy = 1
				End If
			Next
		End If
		
		e\x = e\x + (Cos(e\moveAngle) * e\speed)
		e\y = e\y + (Sin(e\moveAngle) * e\speed)
		
		If e\destroy Then Delete e
	Next
End Function 

Function drawEnemy()
	For e.enemy = Each enemy
		Color e\r, e\g, e\b
		Oval e\x, e\y, e\size, e\size
		If e\dead = 0 Then 
			Color 0, 0, 0
			Rect e\x+3, e\y+3, 2, 2
		End If
		
		If e\pickedUp Then
			drawLine(e\x+2, e\y+2, e\x + Cos(e\moveAngle)*32, e\y + Sin(e\moveAngle)*32, 4, 2)
		End If
	Next
End Function

Type portal
	Field x%
	Field y%
	
	Field lifeTime%
	Field hasSpawned
	
	Field lineAngle#
	
	Field size%
	
	Field destroy
End Type

Function addPortal(x2%, y2%)
	p.portal = New portal
	p\x = x2
	p\y = y2
	
	p\size = 8
End Function

Function updatePortal()
	For p.portal = Each portal
		p\lineAngle = p\lineAngle - 4
		If p\lineAngle <= -360 Then p\lineAngle = 0 
		
		p\lifeTime = p\lifeTime + 1
		
		If p\lifeTime >= 128 Then 
			If p\lifeTime Mod 16 = 0 Then 
				p\size = p\size - 2
				p\x = p\x + 1
				p\y = p\y + 1
			End If
			
			If p\size = 2 And p\hasSpawned = 0 Then 
				addEnemy(p\x-6, p\y-6, Rand(0, PURPLE))
				p\hasSpawned = 1
			End If
			
			If p\size <= 0 Then p\destroy = 1
		End If
		
		If p\destroy Then Delete p
	Next
End Function

Function drawPortal()
	For p.portal = Each portal
		Color 88, 88, 192
		Oval p\x, p\y, p\size, p\size
		Color 204, 172, 112
		drawLine(p\x+p\size/2-1, p\y+p\size/2-1, (p\x+p\size/2-1) + Cos(p\lineAngle)*p\size/2, (p\y+p\size/2-1) + Sin(p\lineAngle)*p\size/2, 2, 0)
		drawLine(p\x+p\size/2-1, p\y+p\size/2-1, (p\x+p\size/2-1) + Cos(p\lineAngle-180)*p\size/2, (p\y+p\size/2-1) + Sin(p\lineAngle-180)*p\size/2, 2, 0)
	Next
End Function

Function update()
	If startscreen = 0 Then
		updateTile()
		updateEnemy()
		updatePlayer()
		updateProjectile()
		updatePortal()
	
		updateLevel()
	Else 
		startDelay = startDelay + 1
		If controller = 0 And JoyDown(1) Then controller = 1
		If pressed() And startDelay >= 32 Then
			startScreen = 0
			setupLevel()
		End If
	End If
End Function

Function draw()
	If startscreen = 0 Then
		drawTile()
		drawPortal()
		drawPlayer()
		drawEnemy()
		drawProjectile()
		
		drawPlayerUi()
	Else
		For i = 0 To 5
			DrawImageRect(spritesheet, WIN_W/2 - (8*5) + i*8, WIN_H/2-16, frame(i, 8), frame(1, 8), 8, 8)
		Next
		
		For i = 0 To 9
			DrawImageRect(spritesheet, WIN_W/2 - (8*5) + i*8, WIN_H/2-16+9, frame(i, 8), frame(2, 8), 8, 8)
		Next
		
		For i = 0 To 13
			DrawImageRect(spritesheet, WIN_W/2 - (8*13)/2 + i*8, WIN_H/2+16*2, frame(i, 8), 55, 8, 8)
		Next
	End If 
End Function 

Global spawnCount%
Global maxSpawnCount% = 256

Global currentLevel%
Global currentLevelCount%

Function updateLevel()
	spawnCount = spawnCount + 1
	
	If spawnCount >= maxSpawnCount Then
		Local amount = 1
		
		If currentLevel >= 6 And currentLevel < 12 Then amount = Rand(0, 2)
		If currentLevel >= 12 Then amount = Rand(0, 3)
		
		For i = 0 To amount - 1 
			addPortal(Rand(32, WIN_W-(16*3)), Rand(32, WIN_H-(16*2)))
		Next
		
		spawnCount = 0
	End If
	
	currentLevelCount = currentLevelCount + 1
	If currentLevelCount >= 256 + currentLevel*8 Then
		currentLevel = currentLevel + 1
		maxSpawnCount = maxSpawnCount - 4
		currentLevelCount = 0
	End If
End Function 

Global maxScore

Function setupLevel()
	reset = 0
	
	startDelay = 0
	
	currentLevel = 0
	currentLevelCount = 0
	
	maxSpawnCount = 128+64
	spawnCount = maxSpawnCount - 32

	For po.portal = Each portal
		Delete po
	Next
	
	For pr.projectile = Each projectile
		Delete pr
	Next

	For t.tile = Each tile
		Delete t
	Next
	
	For p.player = Each player
		Delete p
	Next
	
	For e.enemy = Each enemy
		Delete e
	Next
	
	For x = 0 To 1
		For y = 0 To WIN_H/8 - 1 
			addTile(WIN_W-(x*16)-16, y*8, Rand(0, PURPLE))
			addTile(x*16, y*8, Rand(0, PURPLE))
			
			maxScore = maxScore + 2
		Next
	Next
	
	For y = 0 To 1
		For x = 2 To (WIN_W/16)-3
			addTile(x*16, y*8, Rand(0, PURPLE))
			addTile(x*16, WIN_H - (y*8)-8, Rand(0, PURPLE))
			
			maxScore = maxScore + 2
		Next
	Next
	
	addPlayer()
End Function

HidePointer

While Not KeyHit(1)
	Cls 
	WaitTimer(frametimer)
	draw()
	update()
	If reset Then setupLevel()
	Flip
Wend