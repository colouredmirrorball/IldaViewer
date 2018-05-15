/*
 * Hi there. If you read this, it means you are looking at the source code of IldaViewer! Welcome!
 * When I started creating this program, the intent was to create a framework to easily add features
 * and expand functionability. However, I'm not really an experienced programmer. It's very probable
 * that I didn't use the easiest or clearest way to do things. If you spot an improvement, let me know.
 * To help you contribute code, I added comments to most methods. If something isn't clear however,
 * I'd be glad to help you out.
 *
 * See README.txt for a license draft. You can modify and redistribute this program at will, as long 
 * as proper credit is given. Commercial use of this program is permitted, but you can't redistribute
 * and/or modify this program for commercial purposes.
 *
 * -colouredmirrorball
 *
 */


//Libraries: ControlP5, PeasyCam and SDrop
import controlP5.*; 
import peasy.*;
import sojamo.drop.*;  //Requires manual install from http://www.sojamo.de/libraries/drop/


ControlP5 cp5;
PeasyCam cam;
SDrop drop;

ArrayList<Frame> frames;      //All loaded frames
ArrayList<Palette> palettes;  //All used palettes
ArrayList<String> status;     //Text displayed at the bottom; to clear, use status.clear(), to add, use status.add(String)

float depth = 600;            //Cf. width and height

String ildaViewerVersion = "1.1.4 Beta";

/*
 * Most fields and methods are grouped according to which tab they belong to, for clarity.
 * Each tab ("mode") has a class in their IDE tab or .pde file, with a constructor and an update() method.
 * They are initiated when the user enters a specific tab in the program (in ControlP5's controlEvent() callback method)
 * A boolean checks if they are active. When active, their update() method is called in draw(). 
 * In the future, I hope to automate this, but for now you need to put them there manually.
 * You can put it anywhere in Draw if you want the contents of your tab to rotate with PeasyCam, otherwise you 
 * need to put it in between the cam.beginHUD() and cam.endHUD() methods. If necessary, you can have two update() methods
 * like update3D() and updateGUI() or so.
 * The constructor of the tab class is roughly similar to Processing's setup(), and update() to draw().
 *
 */

// Global and default tab fields:

int activeFrame;              //Which frame is currently displayed?
boolean loading = false;      //Don't display while loading a file
boolean autoplay = false;     //By default, don't start playing frames after each other

int lastTime = 0;             //Controls playback speed
int playbackSpeed = 12;

int activePalette = 0;        //Currently selected palette

boolean showBlanking = true;  //Should blanking points be displayed?
boolean showHeader = true;    //Should the header information be displayed?
//boolean proLaseristMode = false;
boolean laserboyMode = false; //Laserboy mode
boolean generateicon = true;  //Should a frame icon be generated? If not, load in the existing one or use the Processing default icon
boolean picUsesPalette = false;//PIC & CAT files have a palette index in addition to RGB values, use this for their colour instead
boolean respectFrameNumberWhenExportingToCat = false;//and leave empty frames in between
boolean removeEmptyCatFrames = false;

int buttoncolour = color(unhex("FF02344D"));  //GUI
int textcolour = color(255, 255, 255);
int activecolour = color(unhex("FF016C9E"));
int mouseovercolour = color(unhex("FF00B4EA"));

int backgroundcolour = color(0, 0, 0);
int iconcolour = color(96, 68, 255);
PGraphics icon;

boolean cp5update = true;    //Careful!

PFont f8;
PFont f10;
PFont f12;
PFont f16;
PFont f20;
PFont f28;

boolean keyHit = false;
boolean mouseClicked = false;

MultiList importFile;


public void setup()
{
  int size = 600;
  String renderer = OPENGL;
  String defpalette = "Random";
  String font = "Arial";



  //Read the ini file:
  String setupargs[];
  setupargs = loadStrings("IldaViewer.ini");

  saveStrings("setupargs.txt", args);

  if (setupargs == null)
  {
    println("error reading ini file! Please make sure IldaViewer.ini exists in the data folder");
  } else
    for (String setupstring : setupargs)
  {
    String[] q = splitTokens(setupstring);
    if (q.length >= 2)
    {
      if (q[0].equals("size") ) size = int(q[1]);
      if (q[0].equals("renderer") ) renderer = q[1];
      if (q[0].equals("font") ) 
      {
        font = "";
        String[] fnt = splitTokens(q[1], "_");
        font = fnt[0];
        if (fnt.length > 1)
        {
          for (int i = 1; i < fnt.length; i++)
          {
            font += " " + fnt[i];
          }
        }
      }
      if (q[0].equals("showinformation") ) showHeader = boolean(q[1]);
      if (q[0].equals("showblanking") ) showBlanking = boolean(q[1]);
      if (q[0].equals("generateicon") ) generateicon = boolean(q[1]);
      if (q[0].equals("PICusespalette") ) picUsesPalette = boolean(q[1]);
      if (q[0].equals("respectframenumberwhenexportingtocat") ) respectFrameNumberWhenExportingToCat = boolean(q[1]);
      if (q[0].equals("removeemptycatframes")) removeEmptyCatFrames = boolean(q[1]);
      if (q[0].equals("defaultformat")) exportVersion = int(q[1]);
      if (q[0].equals("includepalette")) includePalette = boolean(q[1]);
      if (q[0].equals("optimise")) optimise = boolean(q[1]);
      if (q[0].equals("defaultpalette") ) defpalette = q[1];
      try {
        if (q[0].equals("buttoncolour") ) buttoncolour = unhex(q[1]);

        if (q[0].equals("textcolour") ) textcolour = unhex(q[1]);

        if (q[0].equals("activecolour") ) activecolour = unhex(q[1]);

        if (q[0].equals("mouseovercolour") ) mouseovercolour = unhex(q[1]);

        if (q[0].equals("backgroundcolour") ) backgroundcolour = unhex(q[1]);
        if (q[0].equals("favoritecolour") ) 
        {
          if (q[1].equals("random")) iconcolour = color(random(255), random(255), random(255));
          else iconcolour = unhex(q[1]);
        }
      }
      catch(Exception e) {    //If an error is thrown, use the default values. That'll teach 'em editing the .ini file in an improper way!
      }
    }
  }

  if (renderer.equals("OPENGL")) size(size, size, OPENGL);
  if (renderer.equals("P3D")) size(size, size, P3D);
  if (!renderer.equals("OPENGL") && !renderer.equals("P3D") ) size(size, size, OPENGL);

  f8 = createFont(font, 8, false);
  f10 = createFont(font, 10, false);
  f12 = createFont(font, 12, false);
  f16 = createFont(font, 16, false);
  f20 = createFont(font, 20, false);
  f28 = createFont(font, 28, false);
  textFont(f12);  


  colorMode(RGB);
  //frame.setResizable(true);    // Looks like PeasyCam causes trouble
  //frame.setLocation(20,20);
  frame.setTitle("Ilda viewer " + ildaViewerVersion);

  generateIcon();

  frames = new ArrayList<Frame>();
  status = new ArrayList<String>();
  palettes = new ArrayList<Palette>();

  //Palette standardPalette = new Palette(defpalette);  //Build the default palette
  if (defpalette.equals("Random")) palettes.add(new Palette(defpalette));
  defpalette = "Palettes/" + defpalette;
  File imageFile = new File(defpalette);
  if (imageFile != null && !imageFile.getName().equals("Random"))
  {
    try {
      Palette standardPalette = new Palette(imageFile.getName());
      PImage img = loadImage(imageFile.getPath());
      for ( int i = 0; i < min (img.width, 256); i++)
      {
        standardPalette.addColour(img.get(i, 0));
      }
      palettes.add(standardPalette);
      status.add("Default palette loaded: " + imageFile.getName());
    }
    catch(Exception e)
    {
      status.add("Invalid palette file, check IldaViewer.ini");
      palettes.add(new Palette("Random"));
    }
  }




  cp5 = new ControlP5(this);
  cp5.setFont(f10);
  cp5.enableShortcuts(); //Enables keyboard shortcuts of ControlP5 controls
  cp5.setColorLabel(textcolour);
  cp5.setColorBackground(buttoncolour);
  cp5.setColorActive(activecolour);
  cp5.setColorForeground(mouseovercolour);
  //cp5.setUpdate(false);

  cam = new PeasyCam(this, width/2, height/2, depth/2, depth*2);

  drop = new SDrop(this);                  //SDrop stuff
  sedl = new SeqeditorDropListener();
  mdl = new MainDropListener();
  bdl = new BufferDropListener();

  drop.addDropListener(sedl);
  drop.addDropListener(mdl);
  drop.addDropListener(bdl);

  smooth();

  initializeControlP5();    // All ControlP5 elements are initiated in this method, which has been transported to the Processing IDE tab "ControlP5" for convenience

  cp5.setAutoDraw(false); //Necessary for use with PeasyCam

  lastTime = millis();    // For autoplay

  StringList hints = new StringList();    //Display some hints on startup.
  hints.append("You can change the window size in the IldaViewer.ini file in the data folder.");
  hints.append("Always look on the bright side of life (without burning you retinas).");
  hints.append("You can move GUI elements around when holding alt should they ever be in your way.");
  hints.append("Drag Ilda files to the program to load them in. You can select more than one, or a whole folder.");
  hints.append("In the sequence editor, the main column contains the frames available in the whole program. Frames in the buffer are only available in the sequence editor.");
  hints.append("Use the right mouse button in the sequence editor to multiselect frames.");
  hints.append("Use shift + right mouse button in the sequence editor to select a sequence of frames.");
  hints.append("You can drag and drop Ilda files to the main or buffer column in the sequence editor from a file browser.");
  hints.append("Use the buffer in the sequence editor for temporary storage of frames.");
  //hints.append("Select which editing options you need in the Control Panels menu of the frame editor to tidy up the workspace.");
  //hints.append("You can import a raster image in the frame editor to manually trace and recolour ilda frames.");
  //hints.append("Hold control in the Frame Editor to rotate the view.");
  hints.append("Create a 256x1 image in an external image editor to easily generate and edit palettes.");
  hints.append("You can import all raster image files as palettes (jpg, png, bmp)");
  hints.append("Drag an image to the palette editor to load in a new palette.");
  hints.append("Most Ilda files use the 64Ilda palette. You can set it as default in the ini file.");
  hints.append("Right clicking on an Input node in Oscillabstract resets the value.");
  hints.append("Use the middle mouse button or control key to drag the Oscillabstract workspace around.");
  hints.append("Drag and drop an ilda file or palette image from a file browser to the Oscillabstract workspace to create a Source or Palette element.");
  hints.append("When creating a new connection in Oscillabstract, you can press shift to create more connections at the same time.");
  hints.append("↑↑↓↓←→←→ba (but only in one tab)");
  hints.append("Don't like a feature or have a suggestion? Leave feedback!");

  status.add("Tip: " + hints.get(min((int) random(hints.size()), hints.size()-1)));
  status.add("Initialized");
  println("Ilda Viewer version " + ildaViewerVersion + " booted in " + millis() + " ms.");
}

public void draw()
{
  background(backgroundcolour);



  if (activeFrame >= 0 && activeFrame < frames.size() && !loading)  //Draw the active frame when not loading in a file
  {
    if (cp5.getWindow().getCurrentTab().getName().equals("default") ) frames.get(activeFrame).drawFrame(showBlanking);    //Only draw in the Default tab

    //If the current frame is ilda V0 or V1, display the Palette buttons:
    if ((frames.get(activeFrame).palette) )
    {
      cp5.getController("previousPalette").setVisible(true);
      cp5.getController("nextPalette").setVisible(true);
    } else
    {
      cp5.getController("previousPalette").setVisible(false);
      cp5.getController("nextPalette").setVisible(false);
    }
  }

  // Modes update3D() methods
  // The cam should also be activated in the mousepressed() method for each tab!
  if (sequenceditor) seqeditor.update3D();
  if (seqcreator) sequenceCreator.update3D();

  hint(DISABLE_DEPTH_TEST); //Necessary so text and controls don't rotate around as well
  cam.beginHUD();



  if (loading) drawLoadingScreen();

  // Modes update() methods:
  if (sequenceditor) seqeditor.update();
  if (frameditor) frameEditor.update();
  if (paletteEditor) paleditor.update();
  if (seqcreator) sequenceCreator.update();
  if (exporting) exporter.update();
  if (about) displayAbout();
  displayStatus();
  noStroke();
  noFill();
  cp5.draw();
  hint(ENABLE_DEPTH_TEST);
  //overlay.display();


  cam.endHUD();


  if (autoplay) //Plays and loops all frames in order
  {
    if (playbackSpeed > 0)
    {
      if (millis() - lastTime > 1000/playbackSpeed)
      {
        activeFrame++;
        if (activeFrame >= frames.size() ) activeFrame = 0;
        lastTime = millis();
      }
    }
    if (playbackSpeed < 0)
    {
      if (millis() - lastTime > (-1000/playbackSpeed))
      {
        activeFrame--;
        if (activeFrame < 0 ) activeFrame = frames.size()-1;
        lastTime = millis();
      }
    }
  }

  if (laserboyMode && generateicon)
  {
    try
    {
      if (icon == null) 
      {
        icon = createGraphics(256, 256);
        icon.beginDraw();
        icon.image(loadImage("Images/Icon2.png"), 0, 0);
        icon.endDraw();
      }
      icon.loadPixels();
      for (int i = 0; i < 256; i++)
      {
        for (int j = 0; j < 256; j++)
        {
          icon.pixels[i+256*j] = color(int((sin(float(2)/2+float(frameCount)/15)*0.5+0.5)*255), int((sin(float(2)+PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin(float(2)/2+3*PI/2+float(frameCount)/15)*0.5+0.5)*255), alpha(icon.get(i, j)));
        }
      }
      icon.updatePixels();
      frame.setIconImage(icon.image);
    }
    catch(Exception e)
    {
    }
  }

  keyHit = false;
  mouseClicked = false;
}

void displayStatus() //Displays some information, such as the strings in the status arraylist, fps, file header etc
{
  fill(255);
  textAlign(LEFT);
  textFont(f16);
  for (int i = 0; i < status.size (); i++)
  {
    if (laserboyMode) fill(color(int((sin(float(i)/2+float(frameCount)/15)*0.5+0.5)*255), int((sin(float(i)+PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin(float(i)/2+3*PI/2+float(frameCount)/15)*0.5+0.5)*255))); 
    else fill(255);
    if (textWidth(status.get(i)) > width-10) 
    {
      String[] brokenText = splitTokens(status.get(i));
      String str1 = "";
      String str2 = "";
      int k = 0;
      if (!(textWidth(brokenText[0]) > width-10))
      {
        for (int j = 0; j < brokenText.length; j++)
        {      
          if (textWidth(str1 + brokenText[j] + " ") > width-10)
          {

            k = j;
            j = brokenText.length;
          } else str1 += brokenText[j] + " ";
        }
        status.set(i, str1);
        for (int j = k; j < brokenText.length; j++)
        {      
          str2 += brokenText[j] + " ";
        }
        status.add(i+1, str2);
      }
    }

    text(status.get(i), 10, height-20*status.size()+20*i);
  }
  if (!showHeader) return;
  textAlign(RIGHT);
  if (laserboyMode) fill(color(int((sin(float(1)/2+float(frameCount)/15)*0.5+0.5)*255), int((sin(float(1)+PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin(float(1)/2+3*PI/2+float(frameCount)/15)*0.5+0.5)*255))); 
  else fill(255);
  text("FPS: " + int(frameRate), width-10, 135);
  if (laserboyMode) fill(color(int((sin(float(2)/2+float(frameCount)/15)*0.5+0.5)*255), int((sin(float(2)+PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin(float(2)/2+3*PI/2+float(frameCount)/15)*0.5+0.5)*255))); 
  else fill(255);
  text("Total frames: " + (frames.size()), width-10, 160);
  if ( activeFrame >= 0 && activeFrame < frames.size() && !loading)
  {
    if (laserboyMode) fill(color(int((sin(float(3)/2+float(frameCount)/15)*0.5+0.5)*255), int((sin(float(3)+PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin(float(3)/2+3*PI/2+float(frameCount)/15)*0.5+0.5)*255))); 
    else fill(255);
    text("Point count: " + frames.get(activeFrame).getPointCount(), width-10, 185);
    displayHeader(frames.get(activeFrame).hoofding);
  }
}

void displayHeader(StringList hdr)
{

  textAlign(RIGHT);
  text("Frame information: ", width-10, 220);
  for (int i = 0; i < hdr.size (); i++)
  {
    if (laserboyMode) fill(color(int((sin(float(i)/2+float(frameCount)/15)*0.5+0.5)*255), int((sin(float(i)+PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin(float(i)/2+3*PI/2+float(frameCount)/15)*0.5+0.5)*255))); 
    else fill(255);
    text(hdr.get(i), width-10, 240+20*i);
  }
}

void mousePressed()
{
  mouseClicked = true;
  //Don't interact with PeasyCam while using the GUI
  if (cp5.getWindow().getMouseOverList().isEmpty() && (cp5.getWindow().getCurrentTab().getName().equals("default") || cp5.getWindow().getCurrentTab().getName().equals("seqeditor") ))
  {
    cam.setActive(true);
  } else
  {
    cam.setActive(false);
  }

  // In the Palette Editor tab, check if you clicked on a colour
  if (paletteEditor) paleditor.editPalette(mouseX, mouseY);

  // In the Sequence Creator tab, interact with the non-ControlP5 GUI elements
  if (seqcreator) sequenceCreator.mousePressed();
}

void keyPressed()
{
  keyHit = true;
  /*
  //for PL wiki
   if(key == 'p')
   {
   int i = 0;
   for(PaletteColour c : getActivePalette().colours)
   {
   println("|-");
   println("| " + (i++) + " || " + c.red + " || " + c.green + " || " + c.blue);
   }
   }
   */

  /*
  //for DJDan's javascript based ilda reader
   if (key == 'p')
   {
   for (PaletteColour c : getActivePalette ().colours)
   {
   println("'#" + hex(c.red).charAt(6) + hex(c.green).charAt(6) + hex(c.blue).charAt(6)+"'");
   }
   }
   */

  /*
  //For the Processing library
   if (key == 'p')
   {
   for (PaletteColour c : getActivePalette ().colours)
   {
   println("addColour(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + ");");
   }
   }
   */

  if (key == 's')
  {
    if (frameditor && frameEditor != null)
    {
      EditingFrame frame = new EditingFrame();
      for (int i = 0; i < 500; i++)
      {
        frame.points.add( new EditingPoint(new Point(width*0.5*(1+0.75*sin(float(i)*5*PI/500)*float(i)/500), height*0.5*(1+0.75*cos(float(i)*5*PI/500)*float(i)/500), 0, 255, 255, 255, false)));
      }
      frameEditor.frame = frame;
    }
  }

  if (key == 'c')
  {
    if (frameditor && frameEditor != null)
    {
      if (frameEditor.frame == null) frameEditor.frame = new EditingFrame();
      EditingFrame frame = frameEditor.frame;
      for (int i = 0; i < 50; i++)
      {
        frame.points.add( new EditingPoint(new Point(width*0.5*(0.15*sin(float(i)*2*PI/50))+mouseX, height*0.5*(0.15*cos(float(i)*2*PI/50))+mouseY, 0, 255, 255, 255, false)));
      }
      println("circle at " + mouseX + " " + mouseY);
    }
  }

  if (key == ESC) key = 0;
}



/*
 * All ControlP5 actions get forwarded to this callback method.
 * Some elements don't have a specific callback method, so we have to deal with them here.
 * One such element is the Tab, so initiating a tab or mode class happens here (the "setup" of each tab).
 * 
 *
 */

void controlEvent(ControlEvent theControlEvent) {
  if (theControlEvent.isTab()) {

    //Hide header outside the default tab:
    if (theControlEvent.getTab().getName().equals("default") )
    {
      enterDefaultTab();
    } else
    {
      showHeader = false;
    }

    // Observe the syntax and add your own!
    // When going to a new tab, its corresponding mode boolean should be set to true.
    // When exiting, it should be set to false.

    //Sequence editor tab
    if (theControlEvent.getTab().getName().equals("seqeditor") )
    {
      beginSeqEditor();
    }

    //Exiting Sequence editor tab 
    if (sequenceditor && !theControlEvent.getTab().getName().equals("seqeditor") )
    {
      exitSeqEditor();
    }

    //Frame editor tab
    if (theControlEvent.getTab().getName().equals("framed"))
    {
      beginFrameEditor();
    }

    //Exiting Frame editor tab
    if (frameditor && !theControlEvent.getTab().getName().equals("framed"))
    {
      exitFrameEditor();
    }


    //Palette tab
    if (theControlEvent.getTab().getName().equals("palettes") )
    {
      beginPalettes();
    }

    //Exiting Palette tab 
    if (paletteEditor && !theControlEvent.getTab().getName().equals("palettes") )
    {
      exitPalettes();
    }

    //Sequence Creator tab
    if (theControlEvent.getTab().getName().equals("deluxe") )
    {
      sequenceCreator();
    }

    //Exiting Sequence Creator tab
    if (seqcreator && !theControlEvent.getTab().getName().equals("deluxe") )
    {
      exitSequenceCreator();
    }

    if (theControlEvent.getTab().getName().equals("about") )
    {
      about();
    }

    //Exiting Sequence Creator tab
    if (about && !theControlEvent.getTab().getName().equals("about") )
    {
      exitAbout();
    }


    //Sequence creator tab: split into modes
    if (theControlEvent.isFrom(seqMode))
    {
    }


    //Exporter tab
    if (theControlEvent.getTab().getName().equals("export") )
    {
      beginExporter();
    }

    //Exiting exporter tab
    if (exporting && !theControlEvent.getTab().getName().equals("export") )
    {
      endExporter();
    }
  }

  //Default tab import:
  if (theControlEvent.isFrom(importFile))
  {
    status.clear();
    if (theControlEvent.getValue() == 2) thread("getIldaFile");
    if (theControlEvent.getValue() == 3) thread("loadPicFile");
    if (theControlEvent.getValue() == 4) thread("loadCatFile");
    if (theControlEvent.getValue() == 5) thread("loadImagePaletteFile");
    if (theControlEvent.getValue() == 6) thread("loadTextFile");
    if (theControlEvent.getValue() == 7) thread("loadOscFile");
  }

  //Sequence editor copy mode
  if (theControlEvent.isFrom(copyBehaviour))
  {
    if (copyBehaviour.getValue() == 3) copiedElements.setVisible(true);
    else copiedElements.setVisible(false);
  }

  //Frame editor visible controls
  if (theControlEvent.isFrom(controlsVisible))
  {
    setFrameEditorControls();
  }

  //Palette picker
  if (theControlEvent.isGroup() && theControlEvent.name().equals("myList")) {
    int index = (int)theControlEvent.group().value();
    paleditor.setActivePalette(index);
    activePalette = index;
  }

  //Palette colour picker
  if (theControlEvent.isFrom(cp)) {
    int r = int(theControlEvent.getArrayValue(0));
    int g = int(theControlEvent.getArrayValue(1));
    int b = int(theControlEvent.getArrayValue(2));
    int a = int(theControlEvent.getArrayValue(3));
    color col = color(r, g, b, a);
    if (!paleditor.dontChangeColourMoron)
      paleditor.setActiveColour(col);
  }

  //Sequence creator mode selector
  if (theControlEvent.isFrom(seqMode))
  {
    initiateSeqCreatorMode((int) seqMode.getValue());
    //seqMode.setOpen(!seqMode.isOpen());
    seqMode.setMouseOver(false);
  }

  //Export ilda file version
  if (theControlEvent.isFrom(r)) {
    exportVersion = int(theControlEvent.getValue());
    if (isPaletteFile())
    {
      if (exportVersion == 0 || exportVersion == 1) cp5.getController("includePalette").setVisible(true);
      else cp5.getController("includePalette").setVisible(false);
      if (includePalette || exportVersion == 6 || exportVersion == 7 || exportVersion == 8)
      {
        cp5.getController("nextPaletteExp").setVisible(true);
        cp5.getController("previousPaletteExp").setVisible(true);
        cp5.getController("findBestColour").setVisible(true);
      } else
      {
        cp5.getController("nextPaletteExp").setVisible(false);
        cp5.getController("previousPaletteExp").setVisible(false);
        cp5.getController("findBestColour").setVisible(false);
      }
    } else
    {
      cp5.getController("includePalette").setVisible(false);
      cp5.getController("nextPaletteExp").setVisible(false);
      cp5.getController("previousPaletteExp").setVisible(false);
      cp5.getController("findBestColour").setVisible(false);
    }
  }
}

void enterDefaultTab()
{
  showHeader = boolean(int(cp5.getController("showHeader").getValue()));
  cp5.getController("showBlanking").setValue(float(int(showBlanking)));
  if (frames.size() > 1) cp5.getGroup("framePlayer").setVisible(true);
  if (frames.size() > 0)
  {
    cp5.getController("clearFrames").setVisible(true);
    cp5.getController("showBlanking").setVisible(true);
  }
}

// Convenience method to get the currently active palette

Palette getActivePalette()
{
  if (activePalette >= 0  && activePalette < palettes.size())
  {
    return palettes.get(activePalette);
  } else 
  {
    println("Warning: nonexisting palette selected. (index: " + activePalette + ")" );
    return null;
  }
}

boolean mouseOver(float x1, float y1, float x2, float y2)
{
  return mouseX > x1 && mouseX <= x2 && mouseY > y1 && mouseY <= y2;
}


//      === DEFAULT TAB CP5 CALLBACKS ===

void previousFrame() //manually skip to previous frame
{
  if (activeFrame <= 0)
  {
    activeFrame = frames.size()-1;
    status.clear();
    status.add("Frame: " + (activeFrame+1));
  } else 
  {
    activeFrame--;
    status.clear();
    status.add("Frame: " + (activeFrame+1));
  }
}

void nextFrame() //manually skip to next frame
{
  if (activeFrame >= frames.size()-1 )
  {
    activeFrame = 0;
    status.clear();
    status.add("Frame: " + (activeFrame+1));
  } else
  {
    activeFrame++;
    status.clear();
    status.add("Frame: " + (activeFrame+1));
  }
}

void firstFrame()
{
  activeFrame = 0;
  status.clear();
  status.add("Frame: " + (activeFrame+1));
}

void lastFrame()
{
  if (! frames.isEmpty())
  {
    activeFrame = frames.size()-1;
  } else
  {
    activeFrame = 0;
  }
  status.clear();
  status.add("Frame: " + (activeFrame+1));
}

void autoPlay(boolean autoPLay)
{
  autoplay = !autoplay;
}

void previousPalette() //manually set previous palette active
{
  if (activePalette <= 0)
  {
    activePalette = palettes.size()-1;
  } else 
  {
    activePalette--;
  }
  status.clear();
  status.add("Palette: " + activePalette + " - " + palettes.get(activePalette).name);

  if (activeFrame >= 0 && activeFrame < frames.size())
  {
    frames.get(activeFrame).palettePaint(palettes.get(activePalette));
  }
}

void nextPalette() //manually set next palette active
{
  if (activePalette >= palettes.size()-1)
  {
    activePalette = 0;
  } else
  {
    activePalette++;
  }

  status.clear();
  status.add("Palette: " + activePalette + " - " + palettes.get(activePalette).name);

  if (activeFrame >= 0 && activeFrame < frames.size())
  {
    frames.get(activeFrame).palettePaint(palettes.get(activePalette));
  }
}



void clearFrames()    //Clear all frames and set some controls invisible
{
  frames.clear();
  cp5.getController("clearFrames").setVisible(false);
  cp5.getController("clearFrames").setMouseOver(false);

  cp5.getGroup("framePlayer").setVisible(false);
  cp5.getController("previousPalette").setVisible(false);
  cp5.getController("nextPalette").setVisible(false);
  cp5.getController("showBlanking").setVisible(false);
  activeFrame = 0;

  System.gc();    // Try to get the frames out of your system
  status.clear();
  status.add("All frames cleared.");
}


//Gets called when the load ilda file button is pressed:
public void LoadIldaFile()
{
  //loading = true;    //It's possible frame displaying is in a separate thread, and you can't load and display at the same time
  thread("getIldaFile");
}

void getIldaFile()
{
  selectInput("Load an Ilda file", "selFile");
}

//Gets called when an ilda file is selected in the previous method
void selFile(File selection) {

  //Check if file is valid
  if (selection == null) {
    loading = false;
    status.clear();
    status.add("Window was closed or the user hit cancel.");
    return;
  } else {
    //Check if file exists
    if (!selection.exists())
    {
      loading = false;
      status.clear();
      status.add("Error when trying to read file " + selection.getAbsolutePath());
      status.add("File does not exist.");
      return;
    }
    status.clear();
    status.add("Loading file:");
    status.add(selection.getAbsolutePath());
    //File should be all good now, load it in!
    loading = true;
    loadIldaFile(selection.getAbsolutePath());
  }
}

//Load in the file
void loadIldaFile(String path)
{
  loading = true;    //It's possible frame displaying is in a separate thread, and you can't load and display at the same time
  FileWrapper file = new FileWrapper(path);  //Create a new FileWrapper, this constructor automatically reads the file and adds the frames to the current framelist
  //files.add(file);  //For now, don't store the file
  loading = false;
  if (frames.size() > 1) cp5.getGroup("framePlayer").setVisible(true);
  cp5.getController("clearFrames").setVisible(true);
  cp5.getController("showBlanking").setVisible(true);
}

void drawLoadingScreen()
{
  if (laserboyMode) fill(color(int((sin(float(frameCount)/15)*0.5+0.5)*255), int((sin(PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin(float(frameCount)/15)*0.5+0.5)*255)));
  else fill(255);
  textFont(f20);
  text("Loading", width/2, height/2);
}

void laserboyMode(boolean lbMode)
{
  laserboyMode = lbMode;
  if (lbMode)
  {
    int i = 0;
    for (ControllerInterface conrl : cp5.getAll ())
    {
      if (conrl.getName() != null )
      {
        //Got some weird errors. Looks like try and catch actually works
        try
        {
          conrl.setColorBackground(color(int((sin(float(++i)/30+PI/2)*0.5+0.5)*255), int((cos(float(i)/15+PI)*0.5+0.5)*255), int((sin(float(i)/30+3*PI/2)*0.5+0.5)*255)));
          conrl.setColorLabel(color(int((sin(float(++i)/30+PI/2)*0.5+0.5)*255), int((cos(float(i)/15+PI+PI)*0.5+0.5)*255), int((sin(float(i)/30+3*PI/2-PI)*0.5+0.5)*255)));
          conrl.setColorActive(color(int((sin(float(++i)/30+PI/2)*0.5+0.5)*255), int((cos(float(i)/15+PI)*0.5+0.5)*255), int((sin(float(i)/30+3*PI/2)*0.5+0.5)*255)));
          conrl.setColorForeground(color(int((sin(float(++i)/30+PI/2+PI)*0.5+0.5)*255), int((cos(float(i)/15+PI+PI)*0.5+0.5)*255), int((sin(float(i)/30+3*PI/2+PI)*0.5+0.5)*255)));
        }
        catch(Exception e)
        {
        }
      }
    }

    status.clear();
    status.add("Embrace the rainbow!");
  }

  //else, for, if, try? What's left, while?
  else
  {
    for (ControllerInterface conrl : cp5.getAll ())
    {
      if (conrl.getName() != null )
      {
        try
        {
          conrl.setColorBackground(buttoncolour);
          conrl.setColorLabel(textcolour);
          conrl.setColorActive(activecolour);
          conrl.setColorForeground(mouseovercolour);
        }
        catch(Exception e)
        {
        }
      }
    }
  }

  cp5.getController("playbackSpeed").getCaptionLabel().setVisible(false);
  cp5.getController("playbackSpeed").getValueLabel().setVisible(true);
}

/*
 * Drop support for Ilda files in the main tab:
 */

void dropEvent(DropEvent evt)
{
  if (cp5.getWindow().getCurrentTab().getName().equals("default"))
  {
    if (evt.isFile())
    {
      File f = evt.file();

      if (f.isDirectory())
      {
        status.clear();
        //Load in all the files it can find.
        for (File file : f.listFiles ())
        {
          loadInFile(file);
        }
        loading = false;
      }
      if (f.isFile())
      {
        status.clear();
        if (f != null) loadInFile(f);
        loading = false;
      }
    }
    if (frames.size() > 1) cp5.getGroup("framePlayer").setVisible(true);
    else cp5.getGroup("framePlayer").setVisible(false);
  }
  if (cp5.getWindow().getCurrentTab().getName().equals("palettes") && evt.isImage())
  {
    File f = evt.file();
    paletteSelected(f);
  }
  if (cp5.getWindow().getCurrentTab().getName().equals("deluxe") && creatorMode == 1)
  {
    if (evt.isImage())
    {
      status.clear();
      File f = evt.file();
      if (paleditor == null) paleditor = new PalEditor();
      paletteSelected(f);
      Oscipalette p = new Oscipalette(mouseX, mouseY);
      sequenceCreator.osc.elements.add(p);

      return;
    }
    File f = evt.file();

    if (f.isDirectory())
    {
      status.clear();
      //Load in all the files it can find.
      for (File file : f.listFiles ())
      {
        if (file != null) 
        {
          FileWrapper fw = new FileWrapper(file.getAbsolutePath());
          Oscisource s = new Oscisource(mouseX, mouseY);
          s.firstFrame = frames.size()-1;
          s.sources = fw.getFramesFromBytes();
          s.lastFrame = frames.size()-1;
          sequenceCreator.osc.elements.add(s);
        }
      }
      loading = false;
    }
    if (f.isFile())
    {
      status.clear();
      if (f != null) 
      {
        String extension = getExtension(f);
        if (extension.equalsIgnoreCase(".osc"))
        {
          cp5.getWindow().activateTab("deluxe");

          sequenceCreator();
          oscFileLoad(f);
        }
        if (extension.equalsIgnoreCase(".ild"))
        {
          FileWrapper fw = new FileWrapper(f.getAbsolutePath());
          Oscisource s = new Oscisource(mouseX, mouseY);
          s.firstFrame = frames.size()-1;
          try
          {
            s.sources = fw.getFramesFromBytes();
            s.lastFrame = frames.size()-1;
            sequenceCreator.osc.elements.add(s);
          }
          catch(Exception e)
          {
            //Don't add the Oscisource if something goes wrong
          }
        }
      }

      loading = false;
    }
  }
}

void loadOscFile()
{
  status.clear();
  cp5.getWindow().activateTab("deluxe");
  sequenceCreator();
  if (sequenceCreator.osc.unsaved) status.add("Warning! The current workspace is unsaved. Loading a file will overwrite without saving.");
  selectInput("Select an Oscillabstract workspace file (.osc)", "oscFileSelected");
}

void oscFileSelected(File f)
{

  oscFileLoad(f);
}

void loadInFile(File file)
{
  String extension = getExtension(file);
  if (extension == null) return;
  if (extension.equalsIgnoreCase(".ild")) loadIldaFile(file.getAbsolutePath());
  if (extension.equalsIgnoreCase(".pic")) picFileSelected(file);
  if (extension.equalsIgnoreCase(".cat")) catFileSelected(file);
  if (extension.equalsIgnoreCase(".png")) paletteImageSelected(file);
  if (extension.equalsIgnoreCase(".bmp")) paletteImageSelected(file);
  if (extension.equalsIgnoreCase(".jpg")) paletteImageSelected(file);
  if (extension.equalsIgnoreCase(".txt")) textFileSelected(file);
  if (extension.equalsIgnoreCase(".osc")) oscFileSelected(file);
}


String getExtension(File file)
{
  if (file != null)
  {
    String name = file.getName();
    char[] ext = {
      name.charAt(name.length()-4), name.charAt(name.length()-3), name.charAt(name.length()-2), name.charAt(name.length()-1)
      };
      return new String(ext);
  }
  return null;
}

void generateIcon()
{
  PGraphics p = createGraphics(256, 256);
  p.image(loadImage("Images/Icon2.png"), 0, 0);
  color o = p.get(50, 50);
  if (o != color(red(iconcolour), green(iconcolour), blue(iconcolour), alpha(o)) && generateicon)
  {
    int x1 = 75;
    int y1 = 65;
    int x2 = 181;
    int y2 = 195;
    float decay = 0.0004;
    int thickness = 20;

    p = createGraphics(256, 256);
    p.beginDraw();
    p.smooth();
    p.background(1, 0);

    p.strokeWeight(1);
    p.noFill();

    //The three lines:

    for (int i = x1; i < x2; i++)
    {
      for (int j = -150; j < 150; j++)
      {
        color c = color(red(iconcolour), green(iconcolour), blue(iconcolour), 255*exp(-sq(((float) j))*decay));
        p.set(i, y1+j, c);
      }
    }

    for (int i = y1; i < y2; i++)
    {
      for (int j = -150; j < 150; j++)
      {
        color c = color(red(iconcolour), green(iconcolour), blue(iconcolour), 255*exp(-sq(((float) j))*decay));
        color e = p.get((int) ((x1+x2)*0.5+j), i);
        if (alpha(c) > alpha(e)) p.set((int) ((x1+x2)*0.5+j), i, c);
      }
    }

    for (int i = x1; i < x2; i++)
    {
      for (int j = -150; j < 150; j++)
      {
        color c = color(red(iconcolour), green(iconcolour), blue(iconcolour), 255*exp(-sq(((float) j))*decay));
        color e = p.get(i, y2+j);
        if (alpha(c) > alpha(e)) p.set(i, y2+j, c);
      }
    }

    //The four corners:

    for (int i = -150; i < 0; i++)
    {
      for (int j = -150; j < 150; j++)
      {
        float rsq = sq(i)+sq(j);
        color c = color(red(iconcolour), green(iconcolour), blue(iconcolour), 255*exp(-(rsq)*decay));
        if (alpha(c) > alpha(p.get(x1+i, y1+j))) p.set(x1+i, y1+j, c);
        if (alpha(c) > alpha(p.get(x2-i-1, y1+j)))p.set(x2-i-1, y1+j, c);
        if (alpha(c) > alpha(p.get(x1+i, y2+j)))p.set(x1+i, y2+j, c);
        if (alpha(c) > alpha(p.get(x2-i-1, y2+j)))p.set(x2-i-1, y2+j, c);
      }
    }

    //The white I shape:

    p.strokeWeight(thickness);
    p.stroke(255);
    p.line(x1, y1, x2, y1);
    p.line((x1+x2)*0.5, y1, (x1+x2)*0.5, y2);
    p.line(x1, y2, x2, y2);

    //Make all white pixels transparant:

    for (int i = 0; i < width; i++)
    {
      for (int j = 0; j < height; j++)
      {
        if ( p.get(i, j) == color(255) ) p.set(i, j, color(1, 0));
      }
    }

    p.endDraw();
  }



  frame.setIconImage(p.image);
  p.save("Images/Icon2.png");
}

void loadImagePaletteFile()
{

  status.clear();
  selectInput("Select an image file to load in as a palette", "paletteImageSelected");
}

void paletteImageSelected(File f)
{
  if (f == null) 
  {
    status.add("Error: could not load palette file.");
    return;
  }
  if (paleditor == null) paleditor = new PalEditor();
  paletteSelected(f);
}

void loadPicFile()
{
  status.clear();
  selectInput("Select a PIC file", "picFileSelected");
}

void picFileSelected(File f)
{
  if (f == null || !f.exists())
  {
    status.add("No valid file found.");
    return;
  }
  FileWrapper fw = new FileWrapper(loadBytes(f));
  Frame fr = fw.convertPicToIldaFrame();
  if (fr != null) 
  {
    fr.frameName = f.getName();
    fr.companyName = "Pic2Ilda";
    fr.formHeader();
    frames.add(fr);
    status.add("Found a frame to add!");
  } else
  {
    status.add("Import failed.");
    return;
  }
  if (frames.size() > 1) cp5.getGroup("framePlayer").setVisible(true);
  cp5.getController("clearFrames").setVisible(true);
  cp5.getController("showBlanking").setVisible(true);
}

void loadCatFile()
{
  status.clear();
  selectInput("Select a CAT file", "catFileSelected");
}

void catFileSelected(File f)
{
  if (f == null || !f.exists())
  {
    status.add("No valid file found.");
    return;
  }
  FileWrapper fw = new FileWrapper(loadBytes(f));
  ArrayList<Frame> catFrames = fw.convertCatToFrames();
  if (catFrames == null)
  {
    status.add("Error when reading CAT file");
    status.add(f.getAbsolutePath());
    return;
  }
  for (Frame frame : catFrames)
  {
    String name = f.getName();
    try
    {
      String[] s = split(name, '.');
      if (s.length > 1)
      {
        name = s[0];
        for (int i = 1; i < s.length-1; i++)
        {
          name += "." + s[i];
        }
      }
    }
    catch(Exception e) {
    }
    frame.frameName = name;
    frame.companyName = "Cat2Ilda";
    frame.formHeader();
  }
  frames.addAll(catFrames);
  if (frames.size() > 1) cp5.getGroup("framePlayer").setVisible(true);
  cp5.getController("clearFrames").setVisible(true);
  cp5.getController("showBlanking").setVisible(true);
  status.add("Imported " + catFrames.size() + " frames from CAT file");
  status.add(f.getAbsolutePath());
}

void loadTextFile()
{
  status.clear();
  selectInput("Select a text file", "textFileSelected");
}

void textFileSelected(File f)
{
  if (f == null || !f.exists())
  {
    status.add("No valid file found.");
    return;
  }
  String[] text = loadStrings(f);
  ArrayList<Frame> txtFrames = getFramesFromText(text);
  if (txtFrames == null)
  {
    status.add("Error when reading text file");
    status.add(f.getAbsolutePath());
    return;
  }
  for (Frame frame : txtFrames)
  {
    String name = f.getName();
    try
    {
      String[] s = split(name, '.');
      if (s.length > 1)
      {
        name = s[0];
        for (int i = 1; i < s.length-1; i++)
        {
          name += "." + s[i];
        }
      }
    }
    catch(Exception e) {
    }
    frame.frameName = name;
    frame.companyName = "Txt2Ilda";
    frame.formHeader();
  }
  frames.addAll(txtFrames);
  if (frames.size() > 1) cp5.getGroup("framePlayer").setVisible(true);
  cp5.getController("clearFrames").setVisible(true);
  cp5.getController("showBlanking").setVisible(true);
  status.add("Imported " + txtFrames.size() + " frames from text file");
  status.add(f.getAbsolutePath());
}

public ArrayList<Frame> getFramesFromText(String[] text)
{

  ArrayList<Frame> fs = new ArrayList<Frame>();

  boolean threedim = false;
  int colMod = 0;
  boolean shrt = true;
  Frame frame = new Frame();
  int framNum = 0;
  for (int i = 0; i < text.length; i++)
  {
    String s = text[i];
    if (i == text.length-1) s = "frame";  //what a hack lol
    if (s.length() > 0)
    {
      if (s.charAt(0) == '#') //comment
      {
        //println(s);
      } else
      {
        if (splitTokens(s)[0].equals("frame"))
        {
          if (!frame.points.isEmpty()) 
          {
            if (colMod == 0) 
            {
              frame.palettePaint(getActivePalette());
            }
            frame.frameName = "TextFrame";
            frame.companyName = "Txt2Ilda";
            frame.pointCount = frame.points.size();
            frame.frameNumber = framNum++;
            frame.formHeader();
            fs.add(frame);
          }
          frame = new Frame();
          String[] args = splitTokens(s);
          for (int j = 1; j < args.length; j++)
          {
            if (args[j].equals("xy")) threedim = false;
            if (args[j].equals("xyz")) threedim = true;
            if (args[j].equals("palette")) colMod = 0;
            if (args[j].equals("rgb")) colMod = 1;
            if (args[j].equals("hex")) colMod = 2;
            if (args[j].equals("short")) shrt = true;
            if (args[j].equals("unit")) shrt = false;
          }
        } else if (splitTokens(s)[0].equals("palette"))
        {
        } else
        {
          try
          {
            String[] p = splitTokens(s);
            PVector pos = new PVector();
            int pal = 0;
            color c = 0;
            boolean blanked = false;
            int index = 0;
            float x = shrt ? map( float(p[index++]), -32768, 32767, 0, width) : map( float(p[index++]), -1, 1, 0, width) ;
            float y = shrt ? map( float(p[index++]), -32768, 32767, height, 0) : map( float(p[index++]), -1, 1, height, 0) ;
            float z = threedim ? (shrt ? map( float(p[index++]), -32768, 32767, 0, depth) : map( float(p[index++]), -1, 1, 0, depth) ) : depth*0.5;
            pos.set(x, y, z);
            if ( int(p[index]) == -1) blanked = true;
            else
            {

              switch(colMod)
              {
              case 0 : 
                pal = int(p[index++]);
                c = 0;
                break;
              case 1 : 
                c = color(int(p[index++]), int(p[index++]), int(p[index++]));

                break;
              case 2 : 
                String blargh = p[index++];
                String hexC = blargh.charAt(1) == 'x' ? "FF" + blargh.substring(2) : "FF" + blargh;
                c = unhex(hexC);
                break;
              }
            }
            Point point = new Point(pos, blanked, pal);
            point.colour = c;
            frame.points.add(point);
          }
          catch(Exception e)
          {
            println("Error when reading point " + i + " (malformed text file?)");
            e.printStackTrace();
          }
        }
      }
    }
  }
  return fs;
}

boolean inBetween(float value, float min, float max)
{
  return (value >= min && value < max);
}


/*
 * Pro laserist mode no longer supported.
 * Turns out labels might be useful sometimes.
 * http://www.photonlexicon.com/forums/showthread.php/20949-Z-5-Analog-Abstract-Generator?p=272922#post272922
 *
 */

/*
void proLaseristMode(boolean proMode)
 {
 proLaseristMode = proMode;
 if (proMode)
 {
 for (ControllerInterface conrl : cp5.getAll())
 {
 if (conrl.getName() != null )
 {
 //Got some weird errors. Looks like try and catch actually works
 try
 {
 String thename = conrl.getName();
 
 Label theLabel = cp5.getController(thename).getCaptionLabel();
 if (theLabel != null) theLabel.setVisible(false);
 }
 catch(Exception e)
 {
 }
 }
 }
 
 status.clear();
 status.add("Level up!");
 }
 
 //else, for, if, try? What's left, while?
 else
 {
 for (ControllerInterface conrl : cp5.getAll())
 {
 if (conrl.getName() != null )
 {
 try
 {
 String thename = conrl.getName();
 if (!thename.equals("picker-red") && !thename.equals("picker-green") && !thename.equals("picker-blue") && !thename.equals("picker-alpha"))
 {
 Label theLabel = cp5.getController(thename).getCaptionLabel();
 if (theLabel != null) theLabel.setVisible(true);
 }
 }
 catch(Exception e)
 {
 }
 }
 }
 }
 
 cp5.getController("playbackSpeed").getCaptionLabel().setVisible(false);
 cp5.getController("playbackSpeed").getValueLabel().setVisible(true);
 }
 
 */
