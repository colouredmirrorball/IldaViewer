/*
 * The Frame editor tab is currently invisible
 * and will probably never get implemented properly.
 * A real 3D editor is coming up though in a separate program
 * (with a resizable window! hurrah!)
 */

// Fields for Frame Editor tab:

boolean frameditor = false;   //Frame editor mode
FrameEditor frameEditor;
CheckBox controlsVisible;
int[] ctrlsVis;

void beginFrameEditor()
{
  frameditor = true;
  if (frameEditor == null) frameEditor = new FrameEditor();
  setFrameEditorControls();
}

void exitFrameEditor()
{
  frameditor = false;
}

void setFrameEditorControls()
{
  try
  {
    if (ctrlsVis.length != controlsVisible.getArrayValue().length) println("Error: control panel number mismatch");
    float[] cV = controlsVisible.getArrayValue();

    for (int i = 0; i < ctrlsVis.length; i++)
    {
      if ((int) cV[i] != ctrlsVis[i])
      {
        PVector pos = findEmptyGuiSpot();
        switch(i)
        {

        case 0:
          cp5.getGroup("framePicker").setPosition(pos.x, pos.y);
          cp5.getGroup("framePicker").setVisible(boolean((int) cV[i]));
          break;
        case 1:
          cp5.getGroup("rasterImage").setPosition(pos.x, pos.y);
          cp5.getGroup("rasterImage").setVisible(boolean((int) cV[i]));
          break;
        case 2:
          cp5.getGroup("rotationControl").setPosition(pos.x, pos.y);
          cp5.getGroup("rotationControl").setVisible(boolean((int) cV[i]));
          break;
        case 3:
          cp5.getGroup("sizeControl").setPosition(pos.x, pos.y);
          cp5.getGroup("sizeControl").setVisible(boolean((int) cV[i]));
          break;
        case 4:
          cp5.getGroup("positionControl").setPosition(pos.x, pos.y);
          cp5.getGroup("positionControl").setVisible(boolean((int) cV[i]));
          break;
        case 5:
          cp5.getGroup("perspective").setPosition(pos.x, pos.y);
          cp5.getGroup("perspective").setVisible(boolean((int) cV[i]));
          break;
        }
        ctrlsVis[i] = (int) cV[i];
      }
    }
  }
  catch(Exception e)  //necessary because this method gets called upon startup before the GUI elements are initialized
  {
  }
}

PVector findEmptyGuiSpot()
{
  //Ugh! Why can't things be simple! But it works :)
  Object[] crls = cp5.getAll().toArray();
  PVector currPos = new PVector(150, 35);
  for (int i = 0; i < crls.length; i++)
  {
    ControllerInterface conrl;
    if (crls[i] instanceof ControllerInterface)  
    {
      conrl = (ControllerInterface) crls[i];

      if (conrl.getName() != null )
      {
        try
        {
          if (conrl.getTab().getName().equals("framed"))
          {
            if (abs(conrl.getPosition().x -currPos.x) < 20 && abs(conrl.getPosition().y -currPos.y ) < 20  && conrl.isVisible())
            {
              //Position is already taken!
              currPos.x += 130;
              if (currPos.x > width - 140) 
              {
                currPos.y += 100;
                currPos.x = 150;
              }
              i = 0;
            }
          }
        }
        catch(Exception e)
        {
        }
      }
    }
  }
  return currPos;
}

void openFramedImage()
{
  status.clear();
  status.add("Select an image to load in as a background image");
  selectInput("Select an image file to load in as a background image", "backgroundImageSelected");
}

void backgroundImageSelected(File selection) {
  PImage img;


  if (selection == null) {
    status.clear();
    status.add("Window was closed or the user hit cancel.");
    return;
  } else {
    if (!selection.exists())
    {
      status.clear();
      status.add("Error when trying to read file " + selection.getAbsolutePath());
      status.add("File does not exist.");
      return;
    }
    status.add("Loading image " + selection.getAbsolutePath());
    try {
      img = loadImage(selection.getAbsolutePath());
    }
    catch(Exception e)
    {
      status.add("Invalid file");
      return;
    }
    if (img == null) 
    {
      status.add("Error when opening file");
      return;
    }



    if (frameEditor != null) 
    {
      frameEditor.sourceBackgroundImage = img;
      frameEditor.backgroundImage = img;
      thread("setSizeBackgroundFrameEd");
    }
  }
}

void setSizeBackgroundFrameEd()
{
  if (frameEditor.backgroundImage.width < frameEditor.backgroundImage.height && frameEditor.backgroundImage.height > height) frameEditor.backgroundImage.resize(0, height);
  if (frameEditor.backgroundImage.width > frameEditor.backgroundImage.height && frameEditor.backgroundImage.width > width) frameEditor.backgroundImage.resize(width, 0);

  frameEditor.imgsx = frameEditor.backgroundImage.width;
  frameEditor.imgsy = frameEditor.backgroundImage.height;
  frameEditor.imgx = (int) (width*0.5-frameEditor.backgroundImage.width*0.5);
  frameEditor.imgy = (int) (height*0.5-frameEditor.backgroundImage.height*0.5);
}

void hideFramedImage(boolean value)
{
  frameEditor.showBackground = !value;
}

void positionFramedImage()
{
  cursor(MOVE);
  frameEditor.moveImage = true;
}

void listFrameEd(boolean value)
{
  frameEditor.listFrames = value;
  if (value)
  {

    if (!frames.isEmpty())
    {
      frameEditor.frame = new EditingFrame(frames.get(activeFrame));
    }
  }
  int emptySpot = (int) findEmptyGuiSpot().y+150;
  if (emptySpot > height-150) emptySpot = height-150;
  frameEditor.pickerY = emptySpot;
}

void sizeFramedImage(boolean value)
{

  cp5.getController("sizeXFramedImage").setVisible(value);
  cp5.getController("sizeYFramedImage").setVisible(value);
  cp5.getController("sizeXYFramedImage").setVisible(value);
}

void sizeXYFramedImage(float value)
{
  if (frameEditor == null) return;

  if (value> frameEditor.maxs)
  {
    Slider sl = (Slider) cp5.getController("sizeXYFramedImage");
    sl.setRange(sl.getMin(), sl.getMax()+0.01);
    frameEditor.maxs = sl.getMax()*0.9;
    sl.getCaptionLabel().alignX(CENTER);
  }
  if (value < frameEditor.mins)
  {
    Slider sl = (Slider) cp5.getController("sizeXYFramedImage");
    sl.setRange(sl.getMin()-0.01, sl.getMax());
    frameEditor.mins = sl.getMin()*0.9;
    sl.getCaptionLabel().alignX(CENTER);
  }

  frameEditor.resizeBackground();
}

void sizeXFramedImage(float value)
{
  if (frameEditor == null) return;

  if (value> frameEditor.maxsx)
  {
    Slider sl = (Slider) cp5.getController("sizeXFramedImage");
    sl.setRange(sl.getMin(), sl.getMax()+0.01);
    frameEditor.maxsx = sl.getMax()*0.9;
    sl.getCaptionLabel().alignX(CENTER);
  }
  if (value < frameEditor.minsx)
  {
    Slider sl = (Slider) cp5.getController("sizeXFramedImage");
    sl.setRange(sl.getMin()-0.01, sl.getMax());
    frameEditor.minsx = sl.getMin()*0.9;
    sl.getCaptionLabel().alignX(CENTER);
  }

  frameEditor.resizeBackground();
}

void sizeYFramedImage(float value)
{
  if (frameEditor == null) return;

  if (value> frameEditor.maxsy)
  {
    Slider sl = (Slider) cp5.getController("sizeYFramedImage");
    sl.setRange(sl.getMin(), sl.getMax()+0.01);
    frameEditor.maxsy = sl.getMax()*0.9;
    sl.getCaptionLabel().alignX(CENTER);
  }
  if (value < frameEditor.minsy)
  {
    Slider sl = (Slider) cp5.getController("sizeYFramedImage");
    sl.setRange(sl.getMin()-0.01, sl.getMax());
    frameEditor.minsy = sl.getMin()*0.9;
    sl.getCaptionLabel().alignX(CENTER);
  }

  frameEditor.resizeBackground();
}

void recolourFramedImage()
{
  if (frameEditor == null) return;
  frameEditor.recolourFromImage();
}

void previousFrameEd() //manually skip to previous frame
{
  previousFrame();
  frameEditor.frame = new EditingFrame(frames.get(activeFrame));
}

void nextFrameEd() //manually skip to next frame
{
  nextFrame();
  frameEditor.frame = new EditingFrame(frames.get(activeFrame));
}

void firstFrameEd()
{
  firstFrame();
  frameEditor.frame = new EditingFrame(frames.get(activeFrame));
}

void lastFrameEd()
{
  lastFrame();
  frameEditor.frame = new EditingFrame(frames.get(activeFrame));
}

void rotationX(float value)
{
  if (frameEditor != null) frameEditor.rotX = value;
}

void rotationY(float value)
{
  if (frameEditor != null) frameEditor.rotY = value;
}

void rotationZ(float value)
{
  if (frameEditor != null) frameEditor.rotZ = value;
}

void zoom(float value)
{
  if (frameEditor != null) 
  {
    frameEditor.zoomFactor = value;
    frameEditor.zooming = true;
  }
}

void resetZoom()
{
  if (frameEditor != null) frameEditor.scale = 1;
  cp5.getController("zoom").setValue(0);
}

void viewPerspective(float value)
{
  if (frameEditor != null)
  {
    status.clear();
    if (value == 0) status.add("Orthographic mode");
    else status.add("Perspective mode");

    frameEditor.perspective = value;
  }
}

class FrameEditor
{
  EditingFrame frame;
  PImage backgroundImage, sourceBackgroundImage;
  boolean showBackground = true;
  boolean moveImage = false;
  boolean movingImage = false;
  int movingImageOffX, movingImageOffY, imgx, imgy, imgsx, imgsy;
  float maxsx = 1.25;
  float maxsy = 1.25;
  float minsx = -1.25;
  float minsy = -1.25;
  float maxs = 1.25;
  float mins = -1.25;
  boolean listFrames = false;
  int frameSize = 100;
  int xoffset = 5;
  int visibleFrames = int(width/frameSize);
  int pickedFrame = activeFrame;
  boolean scrolling = false;
  int prevX = 0;
  boolean startscrolling = true;
  int pickerY = 150;
  float rotX, rotY, rotZ;
  float scale = 1;
  float zoomFactor = 0;
  boolean zooming = false;
  float perspective = 0;
  boolean rotating = false;
  float prevRotX, prevRotY;


  FrameEditor()
  {
  }

  FrameEditor(Frame frame)
  {
    this.frame = (EditingFrame) frame;
  }

  void update()
  {
    if (moveImage && mousePressed && !movingImage) 
    {
      movingImage = true;
      movingImageOffX = mouseX - imgx;
      movingImageOffY = mouseY - imgy;
    }
    if (movingImage && !mousePressed)
    {
      movingImage = false;
      moveImage = false;
      cursor(ARROW);
    }
    if (movingImage)
    {
      imgx = mouseX - movingImageOffX;
      imgy = mouseY - movingImageOffY;
    }
    if (backgroundImage != null && showBackground && !listFrames) image(backgroundImage, imgx, imgy);
    if (frame != null && !listFrames) frame.display(0, 0, scale, scale, rotX, rotY, rotZ, perspective);
    if (listFrames)
    {
      listFrames();
    }
    if (zooming) scale += zoomFactor;
    if (zooming && !mousePressed) 
    {
      zooming = false;
      cp5.getController("zoom").setValue(0);
    }
    if (mousePressed && keyPressed && keyCode == CONTROL && !rotating)
    {
      rotating = true;
      prevRotX = mouseY-rotX*width;
      prevRotY = mouseX-rotY*height;
    }
    if (rotating && !mousePressed) rotating = false;
    if (rotating)
    {
      rotX = (mouseY - prevRotX)/width;
      rotY = (mouseX - prevRotY)/height;
      cp5.getController("rotationX").setValue(rotX);
      cp5.getController("rotationY").setValue(rotY);
    }
  }


  void resizeBackground()
  {
    if (sourceBackgroundImage == null) return;

    int newSizeX = (int) (imgsx*cp5.getController("sizeXYFramedImage").getValue()*cp5.getController("sizeXFramedImage").getValue());
    int newSizeY = (int) (imgsy*cp5.getController("sizeXYFramedImage").getValue()*cp5.getController("sizeYFramedImage").getValue());

    imgx += (backgroundImage.width-abs(newSizeX))*0.5;
    imgy += (backgroundImage.height-abs(newSizeY))*0.5;

    int sx = min(0, imgx);
    int sy = min(0, imgy);

    int ssx = min(sourceBackgroundImage.width, width);
    int ssy = min(sourceBackgroundImage.height, height);

    backgroundImage = createImage(abs(newSizeX), abs(newSizeY), RGB);
    backgroundImage.copy(sourceBackgroundImage, sx, sy, ssx, ssy, 0, 0, abs(newSizeX), abs(newSizeY));

    if (newSizeX < 0)
    {
      PImage throwaway = createImage(backgroundImage.width, backgroundImage.height, RGB);
      throwaway.copy(backgroundImage, 0, 0, backgroundImage.width, backgroundImage.height, 0, 0, backgroundImage.width, backgroundImage.height);

      throwaway.loadPixels();
      backgroundImage.loadPixels();

      for (int j = 0; j < backgroundImage.height; j++)
      {
        for (int i = 0; i < backgroundImage.width; i++)
        {

          backgroundImage.pixels[backgroundImage.width-i-1 + j*backgroundImage.width] = throwaway.pixels[i+j*backgroundImage.width];
        }
      }
    }

    if (newSizeY < 0)
    {
      PImage throwaway = createImage(backgroundImage.width, backgroundImage.height, RGB);
      throwaway.copy(backgroundImage, 0, 0, backgroundImage.width, backgroundImage.height, 0, 0, backgroundImage.width, backgroundImage.height);

      throwaway.loadPixels();
      backgroundImage.loadPixels();

      for (int j = 0; j < backgroundImage.height; j++)
      {
        for (int i = 0; i < backgroundImage.width; i++)
        {

          backgroundImage.pixels[i + (backgroundImage.height-1-j)*backgroundImage.width] = throwaway.pixels[i+j*backgroundImage.width];
        }
      }
    }

    backgroundImage.updatePixels();
  }

  void recolourFromImage()
  {
    //println(imgx, imgy);

    backgroundImage.loadPixels();
    println(backgroundImage.pixels.length + " " + backgroundImage.width + " " + backgroundImage.height + " " + imgx + " " + imgy);
    for (EditingPoint point : frame.points)
    {

      //println((int) (point.position.x+imgx+width*(point.position.y+imgy)));
      try
      {
        PVector position = point.position;
        PVector positionInImage = new PVector(point.position.x - imgx, point.position.y - imgy);
        int index = (int) (point.position.x+width*(point.position.y ));
        println("index " + index + " of " + backgroundImage.pixels.length + " at " + positionInImage.x + " " + positionInImage.y);
        point.colour = backgroundImage.pixels[index];
        backgroundImage.pixels[index] = 0;
      }
      catch(Exception e)
      {
        point.colour = 0;
      }
    }
    backgroundImage.updatePixels();
  }

  void listFrames()
  {

    textAlign(LEFT);
    fill(50);
    noStroke();
    rect(0, pickerY, width, 150);
    if (!frames.isEmpty())
    {
      for (int i = max ( (int) -xoffset/frameSize, 0); i < min(frames.size(), (int) -xoffset/frameSize+visibleFrames+1); i++)
      {
        fill(0);
        noStroke();
        if (i == pickedFrame)
        {
          fill(0);
          strokeWeight(3);
          stroke(255, 255, 0);
        }

        fill(0);
        rect(xoffset+frameSize*i, pickerY+10, 90, 90);
        frames.get(i).drawFrame((float)xoffset+frameSize*i, pickerY+10, 0.0, 90.0, 90.0, 0.0, false, true);
        if (i == pickedFrame)
        {
          fill(255, 50);
          textFont(f16);
          text("Editing", xoffset + frameSize*i+5, pickerY+30);
        }
      }
    } else
    {
      textFont(f16);
      fill(255, 50);
      text("No frames", 50, pickerY+40);
    }

    if (mousePressed && mouseY > pickerY && mouseY < pickerY+100)
    {
      int chosenFrame = (int) (mouseX-xoffset)/frameSize;
      if (mouseButton == LEFT) 
      {
        pickedFrame = chosenFrame;
        activeFrame = chosenFrame;
        if (activeFrame >= 0 && activeFrame < frames.size()) frame = new EditingFrame(frames.get(activeFrame));
      }
      if (mouseButton == CENTER && !scrolling) 
      {
        scrolling = true;
        prevX = -xoffset + mouseX;
        cursor(MOVE);
      }
    }

    if (!mousePressed && scrolling)
    {
      scrolling = false;
      cursor(ARROW);
    }

    if (scrolling)
    {
      xoffset = mouseX - prevX;
    }

    noStroke();
    textAlign(CENTER);

    fill(127);  
    rect(10, pickerY+110, 35, 25);
    fill(0);
    textFont(f20);
    text("<<", 25, pickerY+128);
    if (mousePressed && mouseX > 10 && mouseX < 45 && mouseY > pickerY+110 && mouseY <pickerY+135) xoffset = 5;

    fill(127);  
    rect(50, pickerY+110, 35, 25);
    fill(0);
    textFont(f20);
    text("<", 65, pickerY+128);
    if (mousePressed && mouseX > 50 && mouseX < 85 && mouseY > pickerY+110 && mouseY <pickerY+135) xoffset += 5;

    fill(127);  
    rect(width-85, pickerY+110, 35, 25);
    fill(0);
    textFont(f20);
    text(">", width-67, pickerY+128);
    if (mousePressed && mouseX > width-85 && mouseX < width-50 && mouseY > pickerY+110 && mouseY <pickerY+135) xoffset -= 5;

    fill(127);  
    rect(width-45, pickerY+110, 35, 25);
    fill(0);
    textFont(f20);
    text(">>", width-27, pickerY+128);
    if (mousePressed && mouseX > width-45 && mouseX < width-10 && mouseY > pickerY+110 && mouseY <pickerY+135) xoffset = -frames.size() * frameSize + frameSize * visibleFrames;

    fill(127);
    float scrollX = map(xoffset, 0, -frames.size()*frameSize, 100, width-100);
    rect(scrollX, pickerY+110, 10, 25);      
    if (mousePressed && mouseX > scrollX && mouseX < scrollX+10 && mouseY > pickerY+110 && mouseY <pickerY+135) startscrolling = true;
    if (startscrolling)
    {
      xoffset = (int) map(mouseX, 100, width-100, 0, (float)-frames.size()*frameSize);
      if (xoffset > 5) xoffset = 5;
      if (xoffset < -frames.size()*frameSize+ frameSize * visibleFrames) xoffset = -frames.size()*frameSize+ frameSize * visibleFrames;
    }

    if (startscrolling && !mousePressed) startscrolling = false;
  }
}


class EditingFrame extends Frame
{
  ArrayList<EditingPoint> points = new ArrayList<EditingPoint>();
  EditingFrame()
  {
  }

  EditingFrame(Frame frame)
  {
    super(frame);
    for (int i = 0; i< frame.points.size (); i++)
    {
      points.add(new EditingPoint(frame.points.get(i)));
    }
  }

  void display()
  {
    display(0, 0, width, height, 0, 0, 0, 1);
  }

  void display(int x, int y, float sizex, float sizey, float rotx, float roty, float rotz, float projection)
  {
    //Rescale the angles
    rotx = rotx*TWO_PI;
    roty = roty*TWO_PI;
    rotz = rotz*TWO_PI;

    boolean firstPoint = true;
    float oldpositionx = 0; 
    float oldpositiony = 0;

    float[][] R = calculateRotationMatrix(rotx, roty, rotz);


    for (EditingPoint point : points)
    {
      float xind = point.position.x-width*0.5;
      float yind = point.position.y-height*0.5;
      float zind = point.position.z-depth*0.5;

      float xnew = sizex*(R[0][0]*xind + R[0][1]*yind + R[0][2]*zind);
      float ynew = sizey*(R[1][0]*xind + R[1][1]*yind + R[1][2]*zind);
      if (projection != 0)    //perspective
      {
        float znew = R[2][0]*xind + R[2][1]*yind + R[2][2]*zind;  
        znew = znew / depth;
        xnew *= znew/(projection);
        ynew *= znew/(projection);
      }  

      xnew += width*0.5;
      ynew += height*0.5;

      point.displayPoint(xnew, ynew, 0);

      if (!firstPoint)
      {
        strokeWeight(1);  
        line(xnew, ynew, 0, oldpositionx, oldpositiony, 0);
      } else
      {
        firstPoint = false;
      }
      oldpositionx = xnew;
      oldpositiony = ynew;
    }
  }
}

class EditingPoint extends Point
{
  boolean selected = false;

  EditingPoint(Point point)
  {
    super(point);
  }

  void displayPoint(int x, int y, int z)
  {
    setPointColour();
    point(x, y, z);
  }

  void displayPoint(float x, float y, float z)
  {
    setPointColour();
    point(x, y, z);
  }

  void displayPoint()
  {
    displayPoint(position.x, position.y, position.z);
  }

  void setPointColour()
  {
    //Colour:
    strokeWeight(3);
    int red, green, blue;
    int x, y;
    if (blanked)
    {
      red = 75;
      blue = 75;
      green = 75;
    } else
    {
      red = (colour >> 16) & 0xFF;  // Faster way of getting red(argb)
      green = (colour >> 8) & 0xFF;   // Faster way of getting green(argb)
      blue = colour & 0xFF;          // Faster way of getting blue(argb)
    }

    if (selected) 
    {
      red = int(sin(float(frameCount)/5)*127+128);
      strokeWeight(6);
    }
    stroke(red, green, blue);
  }
}

