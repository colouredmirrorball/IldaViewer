class GuiElement
{
  float x;
  float y;
  float sizex;
  float sizey;
  String name;
  String text;
  boolean active = false;
  boolean visible = true;
  boolean updateValue = false;
  int alpha = 255;

  GuiElement()
  {
  }

  GuiElement(float x, float y, float sizex, float sizey, String name)
  {
    this.x = x;
    this.y = y;
    this.sizex = sizex;
    this.sizey = sizey;
    this.name = name;
  }

  void display(float elx, float ely)
  {
    display(g, elx, ely);
  }

  void display(PGraphics pgr, float elx, float ely)
  {
  }

  void update(float elx, float ely)
  {
  }

  boolean activateElement(float elx, float ely)
  {
    if (mouseX > x + elx && mouseX < x + elx + sizex && mouseY > y + ely && mouseY < y + ely + sizey)
    {
      active = true;
      return true;
    }
    return false;
  }

  void toggle()
  {
  }

  float getValue()
  {
    return 0;
  }

  void setValue(float input)
  {
  }

  void activate(boolean activate)
  {
    updateValue = activate;
  }
}

class GuiNumberBox extends GuiElement
{
  float value = 0;
  float oldValue = 0;
  boolean typingInput = false;
  String input = "";
  int cursorPos = 0;

  GuiNumberBox(float x, float y, float sizex, float sizey, String name)
  {
    super(x, y, sizex, sizey, name);
    value = 0;
  }

  void display(PGraphics pgr, float elx, float ely)
  {
    if (pgr != g) pgr.beginDraw();
    pgr.fill(50, alpha);
    if (active)
    {
      if (laserboyMode) pgr.fill(color(int((sin((ely)/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((y)+PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((elx+ely)/2+3*PI/2+float(frameCount)/15)*0.5+0.5)*255)), alpha); 
      else pgr.fill(127, alpha);
      pgr.stroke(0);
      pgr.strokeWeight(1);
    } else
    {
      if (laserboyMode) pgr.fill(color(int((sin((elx)/2+float(frameCount)/15)*0.5+0.5)*255), 255-int((sin((y)+PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((x+y)/2+3*PI/2+float(frameCount)/15)*0.5+0.5)*255)), alpha); 
      else pgr.fill(50, alpha);
      noStroke();
    }
    pgr.rect(x+elx, y+ely, sizex, sizey);
    if (laserboyMode) pgr.fill(color(255-int((sin((x)/2+float(frameCount)/15)*0.5+0.5)*255), int(255-(sin((y)+PI/2+float(frameCount)/15)*0.5+0.5)*255), 255-int((sin((elx+ely)/2+3*PI/2+float(frameCount)/15)*0.5+0.5)*255)), alpha); 
    else pgr.fill(255, alpha);
    pgr.noStroke();
    pgr.textAlign(LEFT);
    pgr.textFont(f10);
    if (!typingInput) pgr.text(value, x+elx + sizex*0.5-textWidth(str(value))*0.5, y+ely + sizey*0.5+3);
    else 
    {
      pgr.text(input, x+elx + sizex*0.5-textWidth(input)*0.5, y+ely + sizey*0.5+3);
      if (frameCount%60>30) //display caret
        try
      {
        pgr.text("|", x+elx + sizex*0.5-textWidth(input)*0.5 + textWidth(input.substring(0, cursorPos))-2, y+ely + sizey*0.5+3);
      }
      catch(Exception e)
      {
        //sometimes the caret position gets bugged, no need to crash the whole program for that
      }
    }
    if (sizex != 0) pgr.triangle(x+elx+2, y+ely+2, x+elx+2, y+ely+sizey-2, x+elx+sqrt(2)*(sizex/sizey), y+ely+0.5*sizey);
    if (pgr != g) pgr.endDraw();
  }

  void update(float elx, float ely)
  {
    super.update(elx, ely);
    if (!mousePressed) active = false;
    if (mouseClicked && activateElement(elx, ely) && !updateValue)
    {

      updateValue = true;
      oldValue = value;
    }
    if (active) 
    {
      value = (0.00001*pow(-mouseY+y+ely+sizey*0.5, 3)+(oldValue));
      if (keyPressed && keyCode == CONTROL) 
      {
        value = (roundToHalfInt(value));
        updateValue = false;
        oldValue = value;
        active = false;
      }
    }
    if (updateValue && !mousePressed) 
    {
      if (activateElement(elx, ely))
      {
        typingInput = true;
        value = oldValue;
        input = str(value);
        cursorPos = input.length();
      }
      updateValue = false;
      active = false;
    }
    if (typingInput && mouseClicked) typingInput = false;    //cancel the action when clicked somewhere else
    if (typingInput && keyHit)
    {
      try
      {
        if (key == ENTER || key == RETURN)
        {
          value = float(input);
          if (Float.isNaN(value)) value = 0;
          typingInput = false;
        }
        if (key >= '0' && key <= '9')
        {
          input = input.substring(0, cursorPos) + str(key) + input.substring(cursorPos++, input.length());
        }
        if (key == 44 || key == 46) input = input.substring(0, cursorPos) + "." + input.substring(cursorPos++, input.length());
        if (key == 45) 
        {
          value = -float(input);
          input = str(value);
        }
        if (key == BACKSPACE) input = input.substring(0, max(0, cursorPos-1)) + input.substring(max(0, cursorPos <= 0 ? 0 : cursorPos--), input.length());
        if (key == DELETE) input = input.substring(0, max(0, cursorPos)) + input.substring(min(input.length(), max(0, cursorPos+1)), input.length());
        if (key == CODED && keyCode == RIGHT) cursorPos = min(++cursorPos, input.length());
        if (key == CODED && keyCode == LEFT) cursorPos = max(0, --cursorPos);
        if (key == CODED && keyCode == UP) 
        {
          value = float(input) + 0.01;
          input = str(value);
        }
        if (key == CODED && keyCode == DOWN) 
        {
          value = float(input) - 0.01;
          input = str(value);
        }
        if (key == CODED && keyCode == 33) //page up
        {
          value = float(input) + 1;
          input = str(value);
        }
        if (key == CODED && keyCode == 34) //page down
        {
          value = float(input) - 1;
          input = str(value);
        }
        if (key == 'r') 
        {
          value = random(value);  //randomise when hitting r
          input = str(value);
        }
      }
      catch(Exception e)
      {
      }
    }
  }

  void setValue(float input)
  {
    value = input;
  }

  float getValue()
  {
    return value;
  }
}

class GuiScroller extends GuiElement
{
  float position; //[0..1]
  float scrollerSize = 5;
  boolean scrolling = false;
  float oldValue = 0;

  GuiScroller(float x, float y, float sizex, float sizey, String name)
  {
    super(x, y, sizex, sizey, name);
  }

  void update(float elx, float ely)
  {
    if (active && !scrolling)
    {
      scrolling = true;
      oldValue = position;
    }

    if (scrolling && !mousePressed)
    {
      scrolling = false;
      active = false;
    }

    if (scrolling)
    {
      if (sizey != 0) position = (mouseY - y - ely)/sizey;
      if (position < 0) position = 0;
      if (position > 1) position = 1;

      if (mouseX > x+elx + sizex) mouseX = (int) (x + elx + sizex);
      if (mouseX < x + elx) mouseX = (int) (x + elx);
      if (mouseY > y + ely + sizey) mouseY = (int) (y + ely + sizey);
      if (mouseY < y + ely) mouseY = (int) (y + ely);
    }
  }

  void display(float elx, float ely)
  {
    if (!visible) return;
    if (laserboyMode) fill(color(int((sin((elx)/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((ely)+PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((x+y)/2+3*PI/2+float(frameCount)/15)*0.5+0.5)*255)), alpha); 
    else fill(200, alpha);
    noStroke();
    rect(x+elx, y+ely, sizex, sizey);
    if (laserboyMode) fill(color(255-int((sin((x)/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((y)+PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((elx+ely)/2+3*PI/2+float(frameCount)/15)*0.5+0.5)*255)), alpha); 
    else fill(50, alpha);
    rect(x+elx, y+ely+map(position, 0, 1, 0, sizey-scrollerSize), sizex, scrollerSize);
  }

  float getValue()
  {
    return position;
  }

  void setValue(float input)
  {
    position = map(input, 0, sizey-scrollerSize, 0, 1);
  }
}

class GuiButton extends GuiElement
{


  GuiButton(float x, float y, float sizex, float sizey, String name)
  {
    super(x, y, sizex, sizey, name);
    text = name;
  }


  void display(PGraphics pgr, float elx, float ely)
  {
    if (!visible) return;
    if (pgr != g) pgr.beginDraw();
    if (laserboyMode) pgr.fill(color(int((sin((y)/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((x)+PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((elx+ely)/2+3*PI/2+float(frameCount)/15)*0.5+0.5)*255)), alpha); 
    else pgr.fill(50, alpha);
    pgr.noStroke();
    pgr.rect(x + elx, y + ely, sizex, sizey);
    if (laserboyMode) pgr.fill(color(int((sin((x)/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((y)+PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((elx+ely)/2+3*PI/2+float(frameCount)/15)*0.5+0.5)*255)), alpha); 
    else pgr.fill(255, alpha);
    pgr.textAlign(LEFT);
    pgr.textFont(f10);
    pgr.text(text, x+elx + sizex*0.5-textWidth(text)*0.5, y+ely + sizey*0.5+3);
    if (pgr != g) pgr.endDraw();
  }
}

class GuiToggle extends GuiElement
{
  String text;
  boolean isActive = false;

  GuiToggle(float x, float y, float sizex, float sizey, String name)
  {
    super(x, y, sizex, sizey, name);
    text = name;
  }

  void display(PGraphics pgr, float elx, float ely)
  {
    if (!visible) return;
    if (pgr != g) pgr.beginDraw();
    if (isActive)
    {
      if (laserboyMode) pgr.fill(color(int((sin((x)/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((y)+PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((elx+ely)/2+3*PI/2+float(frameCount)/15)*0.5+0.5)*255)), alpha); 
      else pgr.fill(127, alpha);
      pgr.stroke(0, alpha);
      pgr.strokeWeight(1);
    } else
    {
      if (laserboyMode) pgr.fill(color(255-int((sin((x)/2+float(frameCount)/15)*0.5+0.5)*255), 255-int((sin((y)+PI/2+float(frameCount)/15)*0.5+0.5)*255), 255-int((sin((elx+ely)/2+3*PI/2+float(frameCount)/15)*0.5+0.5)*255)), alpha); 
      else pgr.fill(50, alpha);
      pgr.noStroke();
    }
    pgr.rect(x + elx, y + ely, sizex, sizey);

    if (isActive)
    {
      if (laserboyMode) pgr.fill(color(255-int((sin((x)/2+float(frameCount)/15)*0.5+0.5)*255), 255- int((sin((y)+PI/2+float(frameCount)/15)*0.5+0.5)*255), 255-int((sin((elx+ely)/2+3*PI/2+float(frameCount)/15)*0.5+0.5)*255)), alpha); 
      else pgr.fill(0, alpha);
    } else
    {
      if (laserboyMode) pgr.fill(color(int((sin((x)/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((y)+PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((elx+ely)/2+3*PI/2+float(frameCount)/15)*0.5+0.5)*255)), alpha); 
      else pgr.fill(255, alpha);
    }

    pgr.textFont(f10);
    pgr.textAlign(CENTER);
    //text(text, x+elx + sizex*0.5-textWidth(text)*0.5, y+ely + sizey*0.5+3);
    pgr.text(text, (int) x+elx + sizex*0.5, (int) y+ely + sizey*0.5+3);
    if (pgr != g) pgr.endDraw();
  }

  void toggle()
  {
    isActive = !isActive;
  }

  float getValue()
  {
    if (isActive) return 1;
    else return 0;
  }

  void setValue(float value)
  {
    if (value == 1) isActive = true;
    else isActive = false;
  }
}

class GuiDropdown extends GuiElement
{
  String text;
  boolean dropdown;

  GuiDropdown(float x, float y, float sizex, float sizey, String name, boolean dropdown)
  {
    this(x, y, sizex, sizey, name);
    this.dropdown = dropdown;
  }

  GuiDropdown(float x, float y, float sizex, float sizey, String name)
  {
    super(x, y, sizex, sizey, name);
    text = name;
  }

  void toggle()
  {
    dropdown = !dropdown;
  }

  void display(float elx, float ely)
  {
    if (!visible) return;
    if (laserboyMode) fill(color(int((sin((x)/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((y)+PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((elx+ely)/2+3*PI/2+float(frameCount)/15)*0.5+0.5)*255)), alpha); 
    else fill(0, alpha);
    noStroke();
    if (dropdown) triangle(x+elx+sizex-11, y+ely+10, x+elx+sizex-5, y+ely+10, x+elx+sizex-8, y+ely+5);
    else triangle(x+elx+sizex-11, y+ely+10, x+elx+sizex-5, y+ely+10, x+elx+sizex-8, y+ely+15);
    textFont(f10);
    textAlign(LEFT);
    text(text, x+elx+sizex-textWidth(text)-15, y+ely+15);
  }

  float getValue()
  {
    if (dropdown) return 1;
    else return 0;
  }

  void setValue(float input)
  {
    if (input == 1) dropdown = true;
    else dropdown = false;
  }
}

class GuiClose extends GuiElement
{
  GuiClose(float x, float y)
  {
    super(x, y, 10, 10, "close");
  }

  void display(float elx, float ely)
  {
    textFont(f10);
    textAlign(LEFT);
    if (laserboyMode) fill(color(int((sin((x)/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((y)+PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((elx+ely)/2+3*PI/2+float(frameCount)/15)*0.5+0.5)*255)), alpha); 
    else fill(0, alpha);
    text("X", x+elx, y+ely+10);
  }
}

