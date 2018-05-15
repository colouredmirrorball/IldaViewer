/*      WARNING
 * 
 * Warning: this class has a very high level of spaghettification!
 * ABANDON ALL HOPE YE WHO ENTER HERE
 *
 */

// Fields for Sequence Editor tab:

boolean sequenceditor = false;//Sequence editor mode
SequenceEditor seqeditor;     
String nrOfInsertedFrames = "1";//Amount of frames to add
RadioButton copyBehaviour;     //What should happen when frames are dropped in the lists?
CheckBox copiedElements;      //What data should be copied?
SeqeditorDropListener sedl;
MainDropListener mdl;
BufferDropListener bdl;

//      === SEQUENCE EDITOR TAB METHODS AND CP5 CALLBACKS ===

void beginSeqEditor()
{
  sequenceditor = true;
  if (seqeditor == null) seqeditor = new SequenceEditor();
  else seqeditor.init();

  status.clear();
  status.add("Drag frames around. Hold ctrl to copy, right mouse button to multiselect.");
}

void exitSeqEditor()
{
  frames.clear();
  for (FrameDraggable frame : seqeditor.theFrames)
  {
    frames.add(frame.frame);
  }
  if (seqeditor.highlightedFrame >= 0 && seqeditor.highlightedFrame < frames.size()) activeFrame = seqeditor.highlightedFrame;
  if (frames.size() > 1) cp5.getGroup("framePlayer").setVisible(true);
  sequenceditor = false;
} 

void editorShowBlanking(boolean theValue)
{
  showBlanking = theValue;
}

void insertFrame()
{
  seqeditor.addFrame();
  status.clear();
  status.add("Empty frames created.");
  status.add("Drag the frames to the Main frame list or the Buffer and drop them.");
}

void deleteFrame()
{
  seqeditor.deleteFrame();
  status.clear();
  status.add("Deleted a frame.");
}

void revrs()    //reverse was already taken
{
  seqeditor.reverseFrame();
}

void randomize()
{
  seqeditor.randomizeFrame();
}

void editorLoadIldaFile()
{
  //loading = true;    //It's possible frame displaying is in a separate thread, and you can't load and display at the same time
  thread("addIldaFile");
}

void addIldaFile()
{
  selectInput("Load an Ilda file", "addFile");
}



//Gets called when an ilda file is selected in the previous method
void addFile(File selection) {

  //Check if file is valid
  if (selection == null) {
    status.clear();
    status.add("Window was closed or the user hit cancel.");
    return;
  } else {
    //Check if file exists
    if (!selection.exists())
    {
      status.clear();
      status.add("Error when trying to read file " + selection.getAbsolutePath());
      status.add("File does not exist.");
      return;
    }
    status.clear();
    status.add("Loading file:");
    status.add(selection.getAbsolutePath());
    //File should be all good now, load it in!
    loadAnIldaFile(selection.getAbsolutePath());
  }
}

//Load in the file
void loadAnIldaFile(String path)
{

  FileWrapper file = new FileWrapper(path, true);  //Create a new FileWrapper, this constructor automatically reads the file, second argument doesn't matter just to distinguish it from the other one
  ArrayList<Frame> newFrames = file.getFramesFromBytes();
  for (Frame frame : newFrames)
  {
    seqeditor.loadedFrames.add(new FrameDraggable(frame));
  }
  status.add("Loading completed.");
}

void editorClearFrames()
{
  seqeditor.theFrames.clear();
  status.clear();
  status.add("All frames cleared.");
}

void editorClearBuffer()
{
  seqeditor.bufferFrames.clear();
  status.clear();
  status.add("Buffer frames cleared.");
}

void editorFitPalette()
{
  seqeditor.fitPalette();
  status.clear();
  status.add("Fitted RGB values with palette " + getActivePalette().name);
}

void editorSelectFrames()
{
  seqeditor.selectFrames();
  status.clear();
  status.add("All frames selected.");
}

void editorSelectBuffer()
{
  seqeditor.selectBuffer();
  status.clear();
  status.add("All frames in buffer selected.");
}

void editorDeselectFrames()
{
  seqeditor.deselectFrames();
  status.clear();
  status.add("All frames deselected.");
}

void editorDeselectBuffer()
{
  seqeditor.deselectBuffer();
  status.clear();
  status.add("All frames in buffer deselected.");
}

void graphicView(boolean view)
{
  seqeditor.setGraphics(view);
}



class SequenceEditor
{
  float yoffset = 30;
  float offvel = 0;
  int initMouseX = 0;
  int initMouseY = 0;
  boolean dragging = false;
  boolean trigger = false;
  boolean triggerb = false;
  boolean checkdraginit = false;
  float yoffsetbuf = 30;
  float offvelbuf = 0;
  Frame displayedFrame; 
  int highlightedFrame = activeFrame;
  int highlightedFrameBuf = -1;
  boolean checkdraginitbuf = false;
  boolean checkKeyPressed = false;
  ArrayList<FrameDraggable> theFrames;
  ArrayList<FrameDraggable> draggedFrames;
  ArrayList<FrameDraggable> bufferFrames;
  ArrayList<FrameDraggable> loadedFrames;
  int frameSize = 25;
  int visibleFrames = int((height-30)/frameSize);
  color mainColour = color(127);
  color bufferColour = color(127);
  boolean graphics = false;
  PGraphics overlay;
  PGraphics frameList;
  PGraphics bufferList;

  SequenceEditor()
  {

    theFrames = new ArrayList<FrameDraggable>();
    draggedFrames = new ArrayList<FrameDraggable>();
    bufferFrames = new ArrayList<FrameDraggable>();
    loadedFrames = new ArrayList<FrameDraggable>();

    for (Frame frame : frames)
    {
      theFrames.add(new FrameDraggable(frame));
    }
    if (!frames.isEmpty())
    {
      displayedFrame = frames.get(activeFrame);
      toggleHighlighted(activeFrame);
      toggleHighlightedBuf(-1);
    }

    overlay = createGraphics(120, (visibleFrames+1)*frameSize-15, P2D);
    frameList = createGraphics(120, (visibleFrames+1)*frameSize-15, P3D);
    bufferList = createGraphics(120, (visibleFrames+1)*frameSize-15, P3D);
    overlay.beginDraw();
    for (int i = 0; i < 50; i++)
    {
      overlay.stroke(red(backgroundcolour), green(backgroundcolour), blue(backgroundcolour), 255-i*5);
      //overlay.stroke(0,0,0,255-i*5);
      overlay.line(0, i, 120, i);
      overlay.line(0, (visibleFrames+1)*frameSize-15-i, 120, (visibleFrames+1)*frameSize-15-i);
    }
    overlay.endDraw();
  }

  void init()
  {
    theFrames.clear();
    imageMode(CORNER);

    for (int i = 0; i < frames.size (); i++)
    {  
      theFrames.add(new FrameDraggable(frames.get(i))) ;
    }

    if (!frames.isEmpty()) 
    {
      displayedFrame = frames.get(activeFrame);
      toggleHighlighted(activeFrame);
      toggleHighlightedBuf(-1);
    } else
    {
      displayedFrame = null;
    }
  }

  void update()
  {
    //Dragging from browser:
    sedl.draw();
    mdl.draw();
    bdl.draw();


    //Update all frame positions:
    for (FrameDraggable frame : theFrames)
    {
      frame.updateOffset();
      frame.resetOffset();
    }


    //Update all bufferframe positions
    for (FrameDraggable frame : bufferFrames)
    {
      frame.updateOffset();
      frame.resetOffset();
    }

    //Display all frames in a list
    if (shouldDisplayMain())
    {
      displayMain();
    }

    //Display the buffer when necessary:
    if (shouldDisplayBuffer())
    {
      displayBuffer();
    }

    //Display loaded ilda files:
    if (!loadedFrames.isEmpty())
    {
      fill(127);
      stroke(color(int((sin(float(frameCount)/15)*0.5+0.5)*127+127), int((sin(PI+float(frameCount)/15)*0.5+0.5)*127+127), 127));
      strokeWeight(5);
    } else
    {
      fill(127, 50);
      stroke(127);
      strokeWeight(2);
    }
    rect(width-200, 335, 185, 85);
    if (!loadedFrames.isEmpty())
    {
      fill(0);
      noStroke();
      rect(width-195, 340, 75, 75);
      Frame frame = loadedFrames.get(0).frame;
      frame.drawFrame(width-195, 340, 5, 75, 75, 0);
      textAlign(RIGHT);
      textFont(f20);
      if (laserboyMode) fill(color(int((sin(float(frameCount)/15)*0.5+0.5)*255), int((sin(PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin(+3*PI/2+float(frameCount)/15)*0.5+0.5)*255))); 
      else fill(255);
      text(loadedFrames.size() + " frame" + (loadedFrames.size() == 1 ? "" : "s"), width-20, 365);
      text(frame.frameName, width-20, 400);
    }

    //Clicked in main column:
    if ( mousePressed && mouseX > 20 && mouseX < 140 && !dragging)
    {

      if (!dragging)
      {
        if (!trigger)
        {
          int pickedFrame = (int) (mouseY-30-yoffset)/frameSize;
          if (pickedFrame >= 0 && pickedFrame < theFrames.size())
          { 
            displayedFrame = theFrames.get(pickedFrame).frame;

            if (mouseButton == RIGHT)
            {
              if (keyPressed && keyCode == SHIFT)
              {
                if (highlightedFrame > pickedFrame)
                {
                  for (int j = pickedFrame+1; j < highlightedFrame; j++)
                  {
                    theFrames.get(j).active = true;
                  }
                } else
                {
                  for (int j = highlightedFrame; j < pickedFrame; j++)
                  {
                    theFrames.get(j).active = true;
                  }
                }
              }
              theFrames.get(pickedFrame).toggleActive();
            }
            toggleHighlighted(pickedFrame);
            toggleHighlightedBuf(-1);
          }
        }


        //Stuff
        initMouseX = mouseX;
        initMouseY = mouseY;
        trigger = true;
        if (mouseButton == LEFT) checkdraginit = true;
      }


      offvel = mouseY - pmouseY;
      cam.setActive(false);
    } else
    {
      //Automatic repositioning for global frames:
      if (yoffset < 0 && (-yoffset/frameSize > (theFrames.size() - visibleFrames/2)))
      {
        offvel = 4*log(-yoffset/frameSize-theFrames.size()+visibleFrames/2);
      }
      if (yoffset/frameSize > visibleFrames/2)
      {
        offvel = -log(yoffset/frameSize);
      }
    }

    //When clicked in the buffer:
    if ( mousePressed && mouseX > 160 && mouseX < 280 && !bufferFrames.isEmpty() && !dragging)
    {

      if (!trigger)
      {
        if (!dragging)
        {
          int pickedFrame = (int) (mouseY-30-yoffsetbuf)/frameSize;
          if (pickedFrame >= 0 && pickedFrame < bufferFrames.size()) 
          {
            displayedFrame = bufferFrames.get(pickedFrame).frame;

            if (mouseButton == RIGHT)
            {
              if (keyPressed && keyCode == SHIFT)
              {
                if (highlightedFrameBuf > pickedFrame)
                {
                  for (int j = pickedFrame+1; j < highlightedFrameBuf; j++)
                  {
                    bufferFrames.get(j).toggleActive();
                  }
                } else
                {
                  for (int j = highlightedFrameBuf; j < pickedFrame; j++)
                  {
                    bufferFrames.get(j).toggleActive();
                  }
                }
              }
              bufferFrames.get(pickedFrame).toggleActive();
            }
            toggleHighlighted(-1);
            toggleHighlightedBuf(pickedFrame);
          }
        }

        initMouseX = mouseX;
        initMouseY = mouseY;
        trigger = true;
        if (mouseButton == LEFT) checkdraginitbuf = true;
      }

      offvelbuf = mouseY - pmouseY;
      cam.setActive(false);
    } else
    {
      if (yoffsetbuf < 0 && (-yoffsetbuf/frameSize > (bufferFrames.size() - visibleFrames/2)))
      {
        offvelbuf = 3*log(-yoffsetbuf/frameSize-bufferFrames.size()+visibleFrames/2);
      }
      if (yoffsetbuf/frameSize > visibleFrames/2)
      {
        offvelbuf = -log(yoffsetbuf/frameSize);
      }
    }

    color colour = color(64, 64, 64);

    colour = getCopyBehaviourColour();

    //Start dragging from main:
    if (mousePressed && mouseX>140 && checkdraginit)
    {
      if (!triggerb)
      {

        for (int i = 0; i < theFrames.size (); i++)
        {
          if (theFrames.get(i).active || highlightedFrame == i)
          {
            FrameDraggable frame = new FrameDraggable(theFrames.get(i));
            frame.source = 0;
            frame.originalPosition = i;
            frame.setColour(colour);
            draggedFrames.add(frame);
          }
        }


        if ((!keyPressed) || (keyCode != CONTROL))
        {
          for (int i = theFrames.size ()-1; i >= 0; i--)
          {
            if (theFrames.get(i).active || highlightedFrame == i)
            {
              theFrames.remove(i);
              for (int j = i; j < theFrames.size (); j++)
              {
                theFrames.get(j).addInternalOffset(frameSize);
              }
            }
          }
        }
        dragging = true;
        triggerb = true;
      }
    }

    //Start dragging from buffer:
    if (mousePressed && (mouseX>280 || mouseX < 160) && checkdraginitbuf)
    {
      if (!triggerb)
      {


        for (int i = 0; i < bufferFrames.size (); i++)
        {
          if (bufferFrames.get(i).active || highlightedFrameBuf == i)
          {
            FrameDraggable frame = new FrameDraggable(bufferFrames.get(i));
            frame.source = 1;
            frame.originalPosition = i;
            frame.setColour(colour);
            draggedFrames.add(frame);
          }
        }
        if (!keyPressed || keyCode != CONTROL)
        {
          for (int i = bufferFrames.size ()-1; i>=0; i--)
          {
            if (bufferFrames.get(i).active || highlightedFrameBuf == i)
            {
              bufferFrames.remove(i);
              for (int j = i; j < bufferFrames.size (); j++)
              {
                bufferFrames.get(j).addInternalOffset(frameSize);
              }
            }
          }
        }

        dragging = true;
        triggerb = true;
      }
    }

    //Start dragging from loaded ilda file section:
    if (mousePressed && (mouseX>width-200 && mouseX < width-15) && (mouseY > 335 && mouseY < 420) && !loadedFrames.isEmpty())
    {
      if (!triggerb)
      {
        for (int i = 0; i < loadedFrames.size (); i++)
        {
          FrameDraggable frame = loadedFrames.get(i);
          frame.source = 3;
          frame.originalPosition = i;
          frame.setColour(colour);
          draggedFrames.add(frame);
        }
        if (!keyPressed || keyCode != CONTROL)
        {
          for (int i = loadedFrames.size ()-1; i>=0; i--)
          {
            loadedFrames.remove(i);
          }
        }

        dragging = true;
        cam.setActive(false);
        triggerb = true;
      }
    }




    if (!mousePressed) {
      cam.setActive(true);
      trigger = false;
      triggerb = false;

      //Mouse released while dragging:
      if (dragging)
      {

        for (FrameDraggable frame : draggedFrames)
        {
          frame.setColour(color(64, 64, 64));
        }

        boolean dropped = false;

        //over main:
        if (mouseX > 20 && mouseX < 140 && !draggedFrames.isEmpty())
        {
          droppedOverMain();
          dropped = true;
        }


        //over buffer:
        if (mouseX > 160 && mouseX < 280 && !draggedFrames.isEmpty())
        {
          droppedOverBuffer();
          dropped = true;
        }

        //over Delete button:
        if (mouseX > cp5.getController("deleteFrame").getPosition().x && mouseX < cp5.getController("deleteFrame").getPosition().x + 85 && mouseY > cp5.getController("deleteFrame").getPosition().y && mouseY < cp5.getController("deleteFrame").getPosition().y + 85)
        {
          draggedFrames.clear();
          dropped = true;
        }

        //over reverse button:
        if (mouseX > cp5.getController("revrs").getPosition().x && mouseX < cp5.getController("revrs").getPosition().x + 85 && mouseY > cp5.getController("revrs").getPosition().y && mouseY < cp5.getController("revrs").getPosition().y + 40)
        {
          reverseFrames(draggedFrames);
          draggedFrames.clear();
          dropped = true;
          status.clear();
          status.add("Reversed some frames");
        }

        //over randomize button:
        if (mouseX > cp5.getController("randomize").getPosition().x && mouseX < cp5.getController("randomize").getPosition().x + 85 && mouseY > cp5.getController("randomize").getPosition().y && mouseY < cp5.getController("randomize").getPosition().y + 40)
        {
          randomizeFrames(draggedFrames);
          draggedFrames.clear();
          dropped = true;
          status.clear();
          status.add("Randomized the order of some frames");
        }


        //somewhere else:
        if (!dropped)
        {
          int source = -1;
          if (!draggedFrames.isEmpty()) source = draggedFrames.get(0).source;
          switch (source)
          {
          case 0:
            int i = 0;
            for (FrameDraggable frame : draggedFrames)
            {
              theFrames.add(frame.originalPosition, frame);
            }
            break;
          case 1:
            i = 0;
            for (FrameDraggable frame : draggedFrames)
            {
              bufferFrames.add(frame.originalPosition, frame);
            }
            break;
          case 3:
            i = 0;
            for (FrameDraggable frame : draggedFrames)
            {
              loadedFrames.add(frame.originalPosition, frame);
            }
            break;
          default:
            draggedFrames.clear();
            break;
          }
        }


        dragging = false;
        draggedFrames.clear();
      }
      if (checkdraginit) checkdraginit = false;
      if (checkdraginitbuf) checkdraginitbuf = false;
    }
    for (FrameDraggable f : theFrames)
    {
      f.colour = color(64);
    }
    for (FrameDraggable f : bufferFrames)
    {
      f.colour = color(64);
    }
    drag();



    if (abs(offvel) < 0.25) offvel = 0;
    yoffset += offvel;
    offvel = 0.95*offvel;
    yoffsetbuf += offvelbuf;
    offvelbuf = 0.95*offvelbuf;

    if (!keyPressed) checkKeyPressed = true;
    if (keyPressed && checkKeyPressed)
    { 

      checkKeyPressed = false;
      if (key == DELETE)
      {
        deleteFrame();
      }
    }

    //Display rectangles to indicate to which column (main/buffer) the clear, select all and deselect all buttons work with
    fill(50);
    stroke(127);
    strokeWeight(2);
    rect(width-300, 440, 285, 40);
    rect(width-300, 510, 285, 40);

    textAlign(RIGHT);
    fill(255);
    textFont(f16);
    text("MAIN", width-15, 438);
    text("BUFFER", width-15, 508);
  }

  boolean shouldDisplayMain()
  {
    return ((dragging && !draggedFrames.isEmpty()) && mouseX > 20 && mouseX < 140 || !theFrames.isEmpty());
  }

  color getCopyBehaviourColour()
  {
    int colour = color(64);
    switch(int(copyBehaviour.getValue()))
    {
    case 1 : 
      colour = color(128, 64, 64);
      break;
    case 2 : 
      colour = color(64, 128, 64);
      break;
    case 3  : 
      colour = color(64, 64, 128);
      break;
    }
    return colour;
  }

  void displayMain()
  {
    frameList.beginDraw(); 
    frameList.background(0);
    frameList.fill(50);
    frameList.stroke(mainColour);
    frameList.strokeWeight(2);
    frameList.rect(1, 1, 118, (visibleFrames+1)*frameSize-15);

    for (int i = max ( (int) -yoffset/frameSize, 0); i < min(theFrames.size(), (int) -yoffset/frameSize+visibleFrames+2); i++)
    {
      boolean highlighted = false;
      if (i == highlightedFrame) highlighted = true;
      if (i>=0 && i < theFrames.size()) theFrames.get(i).display(frameList, 0, frameSize*i+yoffset, 255, highlighted, graphics);//int(75*visibleFrames*sin((yoffset/frameSize+i)*PI/(visibleFrames))), highlighted, graphics);
    }
    frameList.textFont(f28);
    frameList.fill(255, 50);
    frameList.textAlign(LEFT);
    frameList.text("MAIN", 5, 10, 115, visibleFrames*frameSize);
    frameList.endDraw();

    image(frameList, 20, 30);
    image(overlay, 20, 30);
  }

  boolean shouldDisplayBuffer()
  {
    return ((dragging && !draggedFrames.isEmpty()) && mouseX > 160 && mouseX < 280 || !bufferFrames.isEmpty());
  }

  void displayBuffer()
  {    
    bufferList.beginDraw(); 
    bufferList.background(0);
    bufferList.fill(50);
    bufferList.stroke(bufferColour);
    bufferList.strokeWeight(2);
    bufferList.rect(1, 1, 118, (visibleFrames+1)*frameSize-15);

    for (int i = max ( (int) -yoffsetbuf/frameSize, 0); i < min(bufferFrames.size(), (int) -yoffsetbuf/frameSize+visibleFrames+2); i++)
    {
      boolean highlighted = false;
      if (i == highlightedFrameBuf) highlighted = true;
      if (i>=0 && i < bufferFrames.size()) bufferFrames.get(i).display(bufferList, 0, frameSize*i+yoffsetbuf, 255, highlighted, graphics);//int(75*visibleFrames*sin((yoffset/frameSize+i)*PI/(visibleFrames))), highlighted, graphics);
    }
    bufferList.textFont(f28);
    bufferList.fill(255, 50);
    bufferList.textAlign(LEFT);
    bufferList.text("BUFFER", 5, 10, 115, visibleFrames*frameSize);
    bufferList.endDraw();

    image(bufferList, 160, 30);
    image(overlay, 160, 30);
  }

  void drag()
  {
    //While dragging
    if (dragging)
    {
      //Display the dragged frames
      int i = 0;
      for (FrameDraggable frame : draggedFrames)
      {
        frame.display(mouseX-60, frameSize*(i++)+mouseY, graphics);
      }

      //When dragged over Main, scroll up or down and/or indicate correct copy action
      if (mouseX > 20 && mouseX < 140)
      {
        if (mouseY < 20)
        {
          //scroll up
          if (!(yoffset/frameSize > visibleFrames/4)) yoffset += 2*(20-mouseY);
        }
        if (mouseY > height-20)
        {
          //scroll down
          if ( !(yoffset < -frameSize*(theFrames.size()-visibleFrames/1.5))) yoffset -= 2*(mouseY-height+20);
        }

        //Scroll the remaining frames down when inserting:
        if (copyBehaviour.getValue() == 0)
        {
          for (int k = 0; k < theFrames.size (); k++)
          {
            if (mouseY >= frameSize*(k+0.5)+yoffset && mouseY < frameSize*(k+1.5)+yoffset)
            {
              for (int j = k; j < theFrames.size (); j++)
              {
                theFrames.get(j).reachValue(frameSize * draggedFrames.size());
              }
            }
          }
        }

        //For another copy behaviour, indicate to which frames the action will be applied
        if (copyBehaviour.getValue() != 0)
        {

          int pickedFrame = int((mouseY - yoffset)/frameSize-0.5);
          //println(pickedFrame);
          if (pickedFrame >= 0 && pickedFrame < theFrames.size())
          {
            for (int k = pickedFrame; k < min (theFrames.size (), pickedFrame+draggedFrames.size()); k++)
            {
              theFrames.get(k).colour = getCopyBehaviourColour();
            }
          }
        }
      }

      //When dragged over buffer, scroll up or down and/or indicate correct copy action
      if (mouseX > 160 && mouseX < 280)
      {
        if (mouseY < 20)
        {
          if (!(yoffsetbuf/frameSize > visibleFrames/4)) yoffsetbuf += 2*(20-mouseY);
        }
        if (mouseY > height-20)
        {
          if ( !(yoffsetbuf < -frameSize*(bufferFrames.size()-visibleFrames/1.5))) yoffsetbuf -= 2*(mouseY-height+20);
        }

        if (copyBehaviour.getValue() == 0)
        {
          for (int k = 0; k < bufferFrames.size (); k++)
          {
            if (mouseY >= frameSize*(k+0.5)+yoffsetbuf && mouseY < frameSize*(k+1.5)+yoffsetbuf)
            {
              for (int j = k; j < bufferFrames.size (); j++)
              {
                bufferFrames.get(j).reachValue(frameSize * draggedFrames.size());
              }
            }
          }
        } else
        {
          int pickedFrame = int((mouseY - yoffsetbuf)/frameSize-0.5);
          //println(pickedFrame);
          if (pickedFrame >= 0 && pickedFrame < bufferFrames.size())
          {
            for (int k = pickedFrame; k < min (bufferFrames.size (), pickedFrame+draggedFrames.size()); k++)
            {
              bufferFrames.get(k).colour = getCopyBehaviourColour();
            }
          }
        }
      }
    }
  }

  void droppedOverMain()
  {
    boolean wasEmpty = false;
    if (theFrames.isEmpty()) wasEmpty = true;


    //int pickedFrame = (int) (mouseY-(frameSize+5)-yoffset)/frameSize;
    int pickedFrame = int((mouseY - yoffset)/frameSize-0.5);

    if (pickedFrame < 0)
    {


      switch ((int) copyBehaviour.getValue())
      {
      case 0:      //0: insert

        theFrames.addAll(0, draggedFrames);
        yoffset = yoffset - draggedFrames.size()*frameSize;

        break;
      case 1:      //1: overwrite
        for (int i = 0; i < draggedFrames.size (); i++)
        {
          if (i+pickedFrame < 0)
          {
            theFrames.add(i, draggedFrames.get(i));
          } else
          {
            if (i-pickedFrame < theFrames.size())
            {                   
              theFrames.set(i-pickedFrame, draggedFrames.get(i));
            } else
            {
              theFrames.add(draggedFrames.get(i));
            }
          }
        }
        break;
      case 2:    //2: merge
        for (int i = 0; i < draggedFrames.size (); i++)
        {
          if (i+pickedFrame < 0)
          {
            theFrames.add(i, draggedFrames.get(i));
          } else
          {
            if (i-pickedFrame < theFrames.size())
            {
              Frame oldFrame = theFrames.get(i-pickedFrame).frame;
              oldFrame.merge(draggedFrames.get(i).frame);
              theFrames.set(i-pickedFrame, new FrameDraggable(oldFrame));
            } else
            {
              theFrames.add(draggedFrames.get(i));
            }
          }
        }
        break;
      case 3:    //3: copy values
        for (int i = 0; i < draggedFrames.size (); i++)
        {
          if (i+pickedFrame < 0)
          {
            theFrames.add(i, draggedFrames.get(i));
          } else
          {
            if (i-pickedFrame < theFrames.size())
            {
              Frame oldFrame = theFrames.get(i-pickedFrame).frame;
              oldFrame.merge(draggedFrames.get(i).frame, copiedElements.getArrayValue());
              theFrames.set(i-pickedFrame, new FrameDraggable(oldFrame));
            } else
            {
              theFrames.add(draggedFrames.get(i));
            }
          }
        }
        break;
      }
    }

    if (pickedFrame >= 0 && pickedFrame < theFrames.size())
    {

      switch ((int) copyBehaviour.getValue())
      {
      case 0:      //0: insert
        theFrames.addAll(pickedFrame, draggedFrames);
        for (int i = pickedFrame; i < theFrames.size (); i++)
        {
          theFrames.get(i).yOffset = 0;
        }
        break;
      case 1:      //1: overwrite
        for (int i = 0; i < draggedFrames.size (); i++)
        {               
          if (i+pickedFrame < theFrames.size())
          {                   
            theFrames.set(i+pickedFrame, draggedFrames.get(i));
          } else
          {
            theFrames.add(draggedFrames.get(i));
          }
        }

        break;
      case 2:    //2: merge
        for (int i = 0; i < draggedFrames.size (); i++)
        {
          if (i+pickedFrame < theFrames.size())
          {
            Frame oldFrame = theFrames.get(i+pickedFrame).frame;
            oldFrame.merge(draggedFrames.get(i).frame);
            theFrames.set(i+pickedFrame, new FrameDraggable(oldFrame));
          } else
          {
            theFrames.add(draggedFrames.get(i));
          }
        }
        break;
      case 3:    //3: copy values
        for (int i = 0; i < draggedFrames.size (); i++)
        {

          if (i+pickedFrame < theFrames.size())
          {
            Frame oldFrame = theFrames.get(i+pickedFrame).frame;
            oldFrame.merge(draggedFrames.get(i).frame, copiedElements.getArrayValue());
            theFrames.set(i+pickedFrame, new FrameDraggable(oldFrame));
          } else
          {
            theFrames.add(draggedFrames.get(i));
          }
        }
        break;
      }
    }

    if (pickedFrame >= theFrames.size())
    {
      theFrames.addAll(theFrames.size(), draggedFrames);
    }



    if (wasEmpty) 
    {
      yoffset = mouseY-25;
      offvel = 0;
    }

    if (draggedFrames.get(0).frame != null) displayedFrame = draggedFrames.get(0).frame;
    draggedFrames.clear();
    highlightedFrame = max(1, min(pickedFrame, theFrames.size()));
    highlightedFrameBuf = -1;
  }

  void droppedOverBuffer()
  {
    boolean wasEmpty = false;
    if (bufferFrames.isEmpty()) wasEmpty = true;
    int pickedFrame = int( (mouseY-yoffsetbuf)/frameSize-0.5);

    if (pickedFrame < 0)
    {
      switch ((int) copyBehaviour.getValue())
      {
      case 0:      //0: insert
        bufferFrames.addAll(0, draggedFrames);
        yoffsetbuf = yoffsetbuf - draggedFrames.size()*frameSize;
        break;
      case 1:      //1: overwrite

        for (int i = 0; i < draggedFrames.size (); i++)
        {
          if (i+pickedFrame < 0)
          {
            bufferFrames.add(i, draggedFrames.get(i));
          } else
          {
            if (i-pickedFrame < bufferFrames.size())
            {                   
              bufferFrames.set(i-pickedFrame, draggedFrames.get(i));
            } else
            {
              bufferFrames.add(draggedFrames.get(i));
            }
          }
        }
        break;
      case 2:    //2: merge
        for (int i = 0; i < draggedFrames.size (); i++)
        {
          if (i+pickedFrame < 0)
          {
            bufferFrames.add(i, draggedFrames.get(i));
          } else
          {
            if (i-pickedFrame < bufferFrames.size())
            {
              Frame oldFrame = bufferFrames.get(i-pickedFrame).frame;
              oldFrame.merge(draggedFrames.get(i).frame);
              bufferFrames.set(i-pickedFrame, new FrameDraggable(oldFrame));
            } else
            {
              bufferFrames.add(draggedFrames.get(i));
            }
          }
        }
        break;
      case 3:    //3: copy values
        for (int i = 0; i < draggedFrames.size (); i++)
        {
          if (i+pickedFrame < 0)
          {
            bufferFrames.add(i, draggedFrames.get(i));
          } else
          {
            if (i-pickedFrame < bufferFrames.size())
            {
              Frame oldFrame = bufferFrames.get(i-pickedFrame).frame;
              oldFrame.merge(draggedFrames.get(i).frame, copiedElements.getArrayValue());
              bufferFrames.set(i-pickedFrame, new FrameDraggable(oldFrame));
            } else
            {
              bufferFrames.add(draggedFrames.get(i));
            }
          }
        }
        break;
      }
    }

    if (pickedFrame >= 0 && pickedFrame < bufferFrames.size())
    {

      switch ((int) copyBehaviour.getValue())
      {
      case 0:      //0: insert
        bufferFrames.addAll(pickedFrame, draggedFrames);
        for (int i = pickedFrame; i < bufferFrames.size (); i++)
        {
          bufferFrames.get(i).yOffset = 0;
        }
        break;
      case 1:      //1: overwrite
        for (int i = 0; i < draggedFrames.size (); i++)
        {               
          if (i+pickedFrame < bufferFrames.size())
          {                   
            bufferFrames.set(i+pickedFrame, draggedFrames.get(i));
          } else
          {
            bufferFrames.add(draggedFrames.get(i));
          }
        }

        break;
      case 2:    //2: merge
        for (int i = 0; i < draggedFrames.size (); i++)
        {
          if (i+pickedFrame < bufferFrames.size())
          {
            Frame oldFrame = bufferFrames.get(i+pickedFrame).frame;
            oldFrame.merge(draggedFrames.get(i).frame);
            bufferFrames.set(i+pickedFrame, new FrameDraggable(oldFrame));
          } else
          {
            bufferFrames.add(draggedFrames.get(i));
          }
        }
        break;
      case 3:    //3: copy values
        for (int i = 0; i < draggedFrames.size (); i++)
        {

          if (i+pickedFrame < bufferFrames.size())
          {
            Frame oldFrame = bufferFrames.get(i+pickedFrame).frame;
            oldFrame.merge(draggedFrames.get(i).frame, copiedElements.getArrayValue());
            bufferFrames.set(i+pickedFrame, new FrameDraggable(oldFrame));
          } else
          {
            bufferFrames.add(draggedFrames.get(i));
          }
        }
        break;
      }
    }

    if (pickedFrame >= bufferFrames.size())
    {
      bufferFrames.addAll(bufferFrames.size(), draggedFrames);
    }

    if (wasEmpty) 
    {
      yoffsetbuf = mouseY-25;
      offvelbuf = 0;
    }



    if (draggedFrames.get(0).frame != null) displayedFrame = draggedFrames.get(0).frame;
    draggedFrames.clear();
    toggleHighlighted(-1);
    toggleHighlightedBuf(-1);
  }


  void addFrame()
  {
    int amount = int(cp5.get(Textfield.class, "nrOfInsertedFrames").getText());
    if (amount > 4096) amount = 4096;
    color colour = color(64, 64, 64);
    switch(int(copyBehaviour.getValue()))
    {
    case 1 : 
      colour = color(128, 64, 64);
      break;
    case 2 : 
      colour = color(64, 128, 64);
      break;
    case 3  : 
      colour = color(64, 64, 128);
      break;
    }
    for (int i = 0; i < amount; i++)
    {
      Frame frame = new Frame();
      frame.ildaVersion = 4;
      frame.frameName = "NewFrame";
      frame.companyName = "IldaViewer";
      frame.pointCount = 0;
      frame.frameNumber = i;
      frame.totalFrames = amount + frames.size();
      frame.scannerHead = 0;
      frame.formHeader();
      dragging = true;
      FrameDraggable aFrame = new FrameDraggable(frame);
      aFrame.source = 2;
      aFrame.colour = colour;
      draggedFrames.add(aFrame);
    }
  }

  void deleteFrame()
  {
    if (highlightedFrame != -1)
    {
      for (int i = theFrames.size ()-1; i >= 0; i--)
      {
        if (theFrames.get(i).active || i == highlightedFrame)
        {
          theFrames.remove(i);
        }
      }
      if (highlightedFrame > 0 && highlightedFrame < theFrames.size())
      { 
        highlightedFrame--;
        displayedFrame = theFrames.get(highlightedFrame).frame;
      }
    }

    if (highlightedFrameBuf != -1)
    {
      for (int i = bufferFrames.size ()-1; i >= 0; i--)
      {
        if (bufferFrames.get(i).active || i == highlightedFrameBuf)
        {
          bufferFrames.remove(i);
        }
      }
      if (highlightedFrameBuf > 0 && highlightedFrameBuf < bufferFrames.size())
      {
        highlightedFrameBuf--;
        displayedFrame = bufferFrames.get(highlightedFrameBuf).frame;
      }
    }
  }

  void reverseFrame()
  {
    ArrayList<FrameDraggable> temp = new ArrayList<FrameDraggable>();
    int last = 0;
    boolean foundLast = false;
    if (highlightedFrame != -1)
    {
      for (int i = theFrames.size ()-1; i >= 0; i--)
      {
        if (theFrames.get(i).active || i == highlightedFrame)
        {
          if (!foundLast) last = i;
          temp.add(theFrames.get(i));
          theFrames.remove(i);
        }
      }
      if (temp.size() == 1)
      {
        status.clear();
        status.add("You can't reverse the order of only one frame.");
      }
      theFrames.addAll(last, temp);
    }

    if (highlightedFrameBuf != -1)
    {
      for (int i = bufferFrames.size ()-1; i >= 0; i--)
      {
        if (bufferFrames.get(i).active || i == highlightedFrameBuf)
        {
          if (!foundLast) last = i;
          temp.add(bufferFrames.get(i));
          bufferFrames.remove(i);
        }
      }
      if (temp.size() == 1)
      {
        status.clear();
        status.add("You can't reverse the order of only one frame.");
      }
      bufferFrames.addAll(last, temp);
    }
    if (temp.size() != 1)
    {
      status.clear();
      status.add("Reversed the order of " + temp.size() + " frames.");
    }
  }

  void randomizeFrame()
  {
    ArrayList<FrameDraggable> temp = new ArrayList<FrameDraggable>();
    int last = 0;
    boolean foundLast = false;
    IntList indices = new IntList();

    if (highlightedFrame != -1)
    {

      for (int i = theFrames.size ()-1; i >= 0; i--)
      {
        if (theFrames.get(i).active || i == highlightedFrame)
        {
          if (!foundLast) last = i;
          temp.add(0, theFrames.get(i));
          theFrames.remove(i);
        }
      }
      if (temp.size() == 1)
      {
        status.clear();
        status.add("You can't randomize the order of only one frame.");
      }
      for (int i = 0; i < temp.size (); i++)
      {
        indices.append(i);
      }
      indices.shuffle();

      for (int i = 0; i < indices.size (); i++)
      {
        theFrames.add(last, temp.get(indices.get(i)));
      }
    }

    if (highlightedFrameBuf != -1)
    {

      for (int i = bufferFrames.size ()-1; i >= 0; i--)
      {
        if (bufferFrames.get(i).active || i == highlightedFrameBuf)
        {
          if (!foundLast) last = i;
          temp.add(0, bufferFrames.get(i));
          bufferFrames.remove(i);
        }
      }
      if (temp.size() == 1)
      {
        status.clear();
        status.add("You can't randomize the order of only one frame.");
      }
      for (int i = 0; i < temp.size (); i++)
      {
        indices.append(i);
      }
      indices.shuffle();
      for (int i = 0; i < indices.size (); i++)
      {
        bufferFrames.add(last, temp.get(indices.get(i)));
      }
    }
    if (temp.size() != 1)
    {
      status.clear();
      status.add("Randomized the order of " + temp.size() + " frames.");
    }
  }

  void reverseFrames(ArrayList<FrameDraggable> inFrames)
  {
    loadedFrames.clear();
    for (int i = 0; i < inFrames.size (); i++)
    {
      loadedFrames.add(inFrames.get(inFrames.size()-i-1));
    }
  }

  void randomizeFrames(ArrayList<FrameDraggable> inFrames)
  {
    loadedFrames.clear();
    IntList indices = new IntList();
    for (int i = 0; i < inFrames.size (); i++)
    {
      indices.append(i);
    }
    indices.shuffle();
    for (int i = 0; i < inFrames.size (); i++)
    {
      loadedFrames.add(inFrames.get(indices.get(i)));
    }
  }

  void toggleHighlighted(int i)
  {
    highlightedFrame = i;
  }

  void toggleHighlightedBuf(int i)
  {    
    highlightedFrameBuf = i;
  }

  void fitPalette()
  {
    if (highlightedFrame != -1)
    {
      for (int i = theFrames.size ()-1; i >= 0; i--)
      {
        if (theFrames.get(i).active || i == highlightedFrame)
        {
          Frame frame = theFrames.get(i).frame;
          for (Point point : frame.points)
          {
            point.paletteIndex = point.getBestFittingPaletteColourIndex(getActivePalette());
          }
        }
      }
    }

    if (highlightedFrameBuf != -1)
    {
      for (int i = bufferFrames.size ()-1; i >= 0; i--)
      {
        if (bufferFrames.get(i).active || i == highlightedFrameBuf)
        {
          Frame frame = bufferFrames.get(i).frame;
          frame.palette = true;
          for (Point point : frame.points)
          {
            point.paletteIndex = point.getBestFittingPaletteColourIndex(getActivePalette());
          }
        }
      }
    }
  }

  void update3D()
  {
    if (displayedFrame != null) displayedFrame.drawFrame(showBlanking);
  }

  void selectFrames()
  {
    for (FrameDraggable frame : theFrames)
    {
      frame.active = true;
    }
  }

  void selectBuffer()
  {
    for (FrameDraggable frame : bufferFrames)
    {
      frame.active = true;
    }
  }

  void deselectFrames()
  {
    for (FrameDraggable frame : theFrames)
    {
      frame.active = false;
    }
  }

  void deselectBuffer()
  {
    for (FrameDraggable frame : bufferFrames)
    {
      frame.active = false;
    }
  }

  void setGraphics(boolean view)
  {
    graphics = view;

    if (view) 
    {
      frameSize = 125;
      yoffset = -frameSize*highlightedFrame;
      yoffsetbuf = -frameSize*highlightedFrameBuf;
      //yoffset *= 5;
      //yoffsetbuf *= 5;
    } else 
    {
      frameSize = 25;
      yoffset = -frameSize*highlightedFrame;
      yoffsetbuf = -frameSize*highlightedFrameBuf;
      //yoffset *= 0.2;
      //yoffsetbuf *= 0.2;
    }
    println(yoffset);
    visibleFrames = int((height-30)/frameSize);
  }
}

class Draggable
{
  float yOffset;
  boolean active;
  float yOffVel;
  int sizeX=120;
  int sizeY;
  color colour;
  int originalPosition;
  int source;

  public Draggable()
  {
    sizeY = 25;
    active = false;
    colour = color(64);
  }

  void display(float x, float y)
  {
    display(x, y, 255, false);
  }

  void display(float x, float y, int alpha)
  {
    display(x, y, alpha, false);
  }

  void display(float x, float y, int alpha, boolean highlighted)
  {
    display(g, x, y, alpha, highlighted);
  }

  void display(PGraphics pg, float x, float y, int alpha, boolean highlighted)
  {
    int r = 255; 
    int g = 255; 
    int b = 255;
    //pg.beginDraw();
    //Highlight selected frames:
    if (active) 
    {
      r =  int(sin(float(frameCount)/5)*127+128);
      g = 50;
      b = 50;     
      pg.strokeWeight(5);
    } else
    {
      pg.strokeWeight(1);
    }

    //Highlight current active frame (the one being displayed):
    if (highlighted)
    {
      b=0;
      pg.strokeWeight(2);
    }

    //Display them boxes with text:
    pg.stroke(r, g, b, alpha);
    pg.fill((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, alpha);
    pg.rect(x, y+yOffset, sizeX, sizeY-5);
    //pg.endDraw();
  }

  void toggleActive()
  {
    active = !active;
  }

  void setInternalOffset(float offset)
  {
    yOffset = offset;
  }

  void updateOffset()
  {
    yOffset += yOffVel;
    if (yOffVel < 0.25) yOffVel = 0;
  }

  void resetOffset()
  {
    if (yOffset!=0) yOffVel = -yOffset*0.25;
  }

  void addInternalOffset(float offset)
  {
    yOffset += offset;
  }

  void reachValue(float offset)
  {
    if (yOffset != offset) yOffVel = (offset - yOffset)*0.25;
  }

  void setColour(color _colour)
  {
    int red = (_colour >> 16) & 0xFF;
    int green = (_colour >> 8) & 0xFF;
    int blue = _colour & 0xFF;
    colour = color(red, green, blue);
  }
}

class FrameDraggable extends Draggable
{
  Frame frame;
  public FrameDraggable(Frame _frame)
  {
    super();
    frame = _frame;
  }

  FrameDraggable(FrameDraggable frame)
  {
    super();
    if (frame.frame != null) this.frame = frame.frame;
  }

  void display(float x, float y, boolean graphical)
  {
    display(x, y, 255, false, graphical);
  }

  void display(float x, float y, int alpha, boolean highlighted, boolean graphical)
  {
    display(g, x, y, alpha, highlighted, graphical);
  }

  void display(PGraphics pg, float x, float y, int alpha, boolean highlighted, boolean graphical)
  {
    if (graphical) sizeY = 120;
    else sizeY = 25;
    super.display(pg, x, y, alpha, highlighted);
    //pg.beginDraw();
    if (graphical)
    {
      pg.fill(0, alpha);
      pg.noStroke();
      pg.rect(x+5, y+5+yOffset, sizeX-10, sizeY-15);

      frame.drawFrame(pg, x+5, y+5+yOffset, 0, sizeX-5, sizeY-15, 0);
    }
    pg.textAlign(CENTER);
    pg.textFont(f10);
    if (laserboyMode) pg.fill(color(int((sin(0.5*y/height+float(frameCount)/15)*0.5+0.5)*255), int((sin(y/height+PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin(0.5*y/height+3*PI/2+float(frameCount)/15)*0.5+0.5)*255)), alpha); 
    else pg.fill(255, alpha);
    pg.text("[" + frame.frameNumber + "]  " + frame.frameName, x+50, y+15+yOffset);
    //pg.endDraw();
  }
}

class MainDropListener extends DropListener {

  boolean overMain = false;

  MainDropListener() {
    // set a target rect for drop event.
    int frameSize = 25;
    int visibleFrames = int((height-30)/frameSize);
    setTargetRect(20, 30, 120, visibleFrames*25-15);
  }

  void draw() {
    if (overMain)
    {
      if (seqeditor != null) 
      {
        seqeditor.mainColour = color( int(sin(float(frameCount)/5)*127+128), 50, 50);
        if (!seqeditor.shouldDisplayMain()) seqeditor.displayMain();
      }
    }
  }

  // if a dragged object enters the target area.
  // dropEnter is called.
  void dropEnter() {
    overMain = true;
  }

  // if a dragged object leaves the target area.
  // dropLeave is called.
  void dropLeave() {
    if (seqeditor != null) seqeditor.mainColour = color( 127);
    overMain = false;
  }

  void dropEvent(DropEvent evt) {
    //Place frames in main list
    if (seqeditor != null && cp5.getWindow().getCurrentTab().getName().equals("seqeditor"))
    {
      overMain = false;

      if (evt.isFile())
      {
        File f = evt.file();
        try
        {

          if (f.isDirectory())
          {
            status.clear();
            //Load in all the files it can find.
            for (File file : f.listFiles ())
            {
              FileWrapper fl = new FileWrapper( file.getAbsolutePath(), true); 
              ArrayList<Frame> newFrames = fl.getFramesFromBytes();
              for (Frame frame : newFrames)
              {
                seqeditor.draggedFrames.add(new FrameDraggable(frame));
              }
            }
            loading = false;
          } else
          {
            FileWrapper fl = new FileWrapper( f.getAbsolutePath(), true); 
            ArrayList<Frame> newFrames = fl.getFramesFromBytes();
            for (Frame frame : newFrames)
            {
              seqeditor.draggedFrames.add(new FrameDraggable(frame));
            }
          }
        }
        catch(Exception e)
        {
          status.add("Something went wrong while dropping files. Try again or go cry.");
        }
      }
      if (seqeditor != null) seqeditor.droppedOverMain();
      status.clear();
      status.add("Frames added to main list.");
    }
  }
}


class BufferDropListener extends DropListener {

  boolean overBuffer = false;

  BufferDropListener() {
    // set a target rect for drop event.
    int frameSize = 25;
    int visibleFrames = int((height-30)/frameSize);
    setTargetRect(160, 30, 120, visibleFrames*25-15);
  }

  void draw() {
    if (overBuffer)
    {
      if (seqeditor != null) 
      {
        seqeditor.bufferColour = color( int(sin(float(frameCount)/5)*127+128), 50, 50);
        if (!seqeditor.shouldDisplayBuffer()) seqeditor.displayBuffer();
      }
    }
  }

  // if a dragged object enters the target area.
  // dropEnter is called.
  void dropEnter() {
    overBuffer = true;
  }

  // if a dragged object leaves the target area.
  // dropLeave is called.
  void dropLeave() {
    if (seqeditor != null) seqeditor.bufferColour = color( 127);
    overBuffer = false;
  }

  void dropEvent(DropEvent evt) {

    //Add frames to the Buffer:
    if (seqeditor != null && cp5.getWindow().getCurrentTab().getName().equals("seqeditor"))
    {
      overBuffer = false;
      if (evt.isFile())
      {
        File f = evt.file();
        try
        {


          if (f.isDirectory())
          {
            status.clear();
            //Load in all the files it can find.
            for (File file : f.listFiles ())
            {
              FileWrapper fl = new FileWrapper( file.getAbsolutePath(), true); 
              ArrayList<Frame> newFrames = fl.getFramesFromBytes();
              for (Frame frame : newFrames)
              {
                seqeditor.draggedFrames.add(new FrameDraggable(frame));
              }
            }
            loading = false;
          }
          if (f.isFile())
          {
            FileWrapper fl = new FileWrapper( f.getAbsolutePath(), true); 
            ArrayList<Frame> newFrames = fl.getFramesFromBytes();
            for (Frame frame : newFrames)
            {
              seqeditor.draggedFrames.add(new FrameDraggable(frame));
            }
          }
        }

        catch(Exception e)
        {
          status.add("Something went wrong while dropping files. Try again or go cry.");
        }
      }
      if (seqeditor != null) seqeditor.droppedOverBuffer();
      status.clear();
      status.add("Frames added to buffer list.");
    }
  }
}

class SeqeditorDropListener extends DropListener {
  boolean dragging = false;
  Draggable draggable = new Draggable();

  SeqeditorDropListener() {
    // set a target rect for drop event.

    setTargetRect(0, 0, width, height);
  }

  void draw() {
    /*
    if (dragging)
     {
     draggable.display(mouseX, mouseY);
     }
     */
  }

  // if a dragged object enters the target area.
  // dropEnter is called.
  void dropEnter() {
    if (cp5.getWindow().getCurrentTab().getName().equals("seqeditor")) 
    {
      if (seqeditor != null) seqeditor.draggedFrames.clear();
      dragging = true;
      status.clear();
      status.add("Drop file in the main or buffer frame list.");
    }
  }

  // if a dragged object leaves the target area.
  // dropLeave is called.
  void dropLeave() {
    dragging = false;
  }

  void dropEvent(DropEvent theEvent) {

    //Don't do anything
  }
}

