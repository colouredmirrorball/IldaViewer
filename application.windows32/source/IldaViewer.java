import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import controlP5.*; 
import peasy.*; 
import sojamo.drop.*; 
import javax.swing.*; 
import java.awt.event.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class IldaViewer extends PApplet {

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
 

  //Requires manual install from http://www.sojamo.de/libraries/drop/


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
      if (q[0].equals("size") ) size = PApplet.parseInt(q[1]);
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
      if (q[0].equals("showinformation") ) showHeader = PApplet.parseBoolean(q[1]);
      if (q[0].equals("showblanking") ) showBlanking = PApplet.parseBoolean(q[1]);
      if (q[0].equals("generateicon") ) generateicon = PApplet.parseBoolean(q[1]);
      if (q[0].equals("PICusespalette") ) picUsesPalette = PApplet.parseBoolean(q[1]);
      if (q[0].equals("respectframenumberwhenexportingtocat") ) respectFrameNumberWhenExportingToCat = PApplet.parseBoolean(q[1]);
      if (q[0].equals("removeemptycatframes")) removeEmptyCatFrames = PApplet.parseBoolean(q[1]);
      if (q[0].equals("defaultformat")) exportVersion = PApplet.parseInt(q[1]);
      if (q[0].equals("includepalette")) includePalette = PApplet.parseBoolean(q[1]);
      if (q[0].equals("optimise")) optimise = PApplet.parseBoolean(q[1]);
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
  hints.append("\u2191\u2191\u2193\u2193\u2190\u2192\u2190\u2192ba (but only in one tab)");
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
          icon.pixels[i+256*j] = color(PApplet.parseInt((sin(PApplet.parseFloat(2)/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(PApplet.parseFloat(2)+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(PApplet.parseFloat(2)/2+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), alpha(icon.get(i, j)));
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

public void displayStatus() //Displays some information, such as the strings in the status arraylist, fps, file header etc
{
  fill(255);
  textAlign(LEFT);
  textFont(f16);
  for (int i = 0; i < status.size (); i++)
  {
    if (laserboyMode) fill(color(PApplet.parseInt((sin(PApplet.parseFloat(i)/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(PApplet.parseFloat(i)+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(PApplet.parseFloat(i)/2+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255))); 
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
  if (laserboyMode) fill(color(PApplet.parseInt((sin(PApplet.parseFloat(1)/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(PApplet.parseFloat(1)+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(PApplet.parseFloat(1)/2+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255))); 
  else fill(255);
  text("FPS: " + PApplet.parseInt(frameRate), width-10, 135);
  if (laserboyMode) fill(color(PApplet.parseInt((sin(PApplet.parseFloat(2)/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(PApplet.parseFloat(2)+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(PApplet.parseFloat(2)/2+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255))); 
  else fill(255);
  text("Total frames: " + (frames.size()), width-10, 160);
  if ( activeFrame >= 0 && activeFrame < frames.size() && !loading)
  {
    if (laserboyMode) fill(color(PApplet.parseInt((sin(PApplet.parseFloat(3)/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(PApplet.parseFloat(3)+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(PApplet.parseFloat(3)/2+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255))); 
    else fill(255);
    text("Point count: " + frames.get(activeFrame).getPointCount(), width-10, 185);
    displayHeader(frames.get(activeFrame).hoofding);
  }
}

public void displayHeader(StringList hdr)
{

  textAlign(RIGHT);
  text("Frame information: ", width-10, 220);
  for (int i = 0; i < hdr.size (); i++)
  {
    if (laserboyMode) fill(color(PApplet.parseInt((sin(PApplet.parseFloat(i)/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(PApplet.parseFloat(i)+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(PApplet.parseFloat(i)/2+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255))); 
    else fill(255);
    text(hdr.get(i), width-10, 240+20*i);
  }
}

public void mousePressed()
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

public void keyPressed()
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
        frame.points.add( new EditingPoint(new Point(width*0.5f*(1+0.75f*sin(PApplet.parseFloat(i)*5*PI/500)*PApplet.parseFloat(i)/500), height*0.5f*(1+0.75f*cos(PApplet.parseFloat(i)*5*PI/500)*PApplet.parseFloat(i)/500), 0, 255, 255, 255, false)));
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
        frame.points.add( new EditingPoint(new Point(width*0.5f*(0.15f*sin(PApplet.parseFloat(i)*2*PI/50))+mouseX, height*0.5f*(0.15f*cos(PApplet.parseFloat(i)*2*PI/50))+mouseY, 0, 255, 255, 255, false)));
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

public void controlEvent(ControlEvent theControlEvent) {
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
    int r = PApplet.parseInt(theControlEvent.getArrayValue(0));
    int g = PApplet.parseInt(theControlEvent.getArrayValue(1));
    int b = PApplet.parseInt(theControlEvent.getArrayValue(2));
    int a = PApplet.parseInt(theControlEvent.getArrayValue(3));
    int col = color(r, g, b, a);
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
    exportVersion = PApplet.parseInt(theControlEvent.getValue());
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

public void enterDefaultTab()
{
  showHeader = PApplet.parseBoolean(PApplet.parseInt(cp5.getController("showHeader").getValue()));
  cp5.getController("showBlanking").setValue(PApplet.parseFloat(PApplet.parseInt(showBlanking)));
  if (frames.size() > 1) cp5.getGroup("framePlayer").setVisible(true);
  if (frames.size() > 0)
  {
    cp5.getController("clearFrames").setVisible(true);
    cp5.getController("showBlanking").setVisible(true);
  }
}

// Convenience method to get the currently active palette

public Palette getActivePalette()
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

public boolean mouseOver(float x1, float y1, float x2, float y2)
{
  return mouseX > x1 && mouseX <= x2 && mouseY > y1 && mouseY <= y2;
}


//      === DEFAULT TAB CP5 CALLBACKS ===

public void previousFrame() //manually skip to previous frame
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

public void nextFrame() //manually skip to next frame
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

public void firstFrame()
{
  activeFrame = 0;
  status.clear();
  status.add("Frame: " + (activeFrame+1));
}

public void lastFrame()
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

public void autoPlay(boolean autoPLay)
{
  autoplay = !autoplay;
}

public void previousPalette() //manually set previous palette active
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

public void nextPalette() //manually set next palette active
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



public void clearFrames()    //Clear all frames and set some controls invisible
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

public void getIldaFile()
{
  selectInput("Load an Ilda file", "selFile");
}

//Gets called when an ilda file is selected in the previous method
public void selFile(File selection) {

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
public void loadIldaFile(String path)
{
  loading = true;    //It's possible frame displaying is in a separate thread, and you can't load and display at the same time
  FileWrapper file = new FileWrapper(path);  //Create a new FileWrapper, this constructor automatically reads the file and adds the frames to the current framelist
  //files.add(file);  //For now, don't store the file
  loading = false;
  if (frames.size() > 1) cp5.getGroup("framePlayer").setVisible(true);
  cp5.getController("clearFrames").setVisible(true);
  cp5.getController("showBlanking").setVisible(true);
}

public void drawLoadingScreen()
{
  if (laserboyMode) fill(color(PApplet.parseInt((sin(PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255)));
  else fill(255);
  textFont(f20);
  text("Loading", width/2, height/2);
}

public void laserboyMode(boolean lbMode)
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
          conrl.setColorBackground(color(PApplet.parseInt((sin(PApplet.parseFloat(++i)/30+PI/2)*0.5f+0.5f)*255), PApplet.parseInt((cos(PApplet.parseFloat(i)/15+PI)*0.5f+0.5f)*255), PApplet.parseInt((sin(PApplet.parseFloat(i)/30+3*PI/2)*0.5f+0.5f)*255)));
          conrl.setColorLabel(color(PApplet.parseInt((sin(PApplet.parseFloat(++i)/30+PI/2)*0.5f+0.5f)*255), PApplet.parseInt((cos(PApplet.parseFloat(i)/15+PI+PI)*0.5f+0.5f)*255), PApplet.parseInt((sin(PApplet.parseFloat(i)/30+3*PI/2-PI)*0.5f+0.5f)*255)));
          conrl.setColorActive(color(PApplet.parseInt((sin(PApplet.parseFloat(++i)/30+PI/2)*0.5f+0.5f)*255), PApplet.parseInt((cos(PApplet.parseFloat(i)/15+PI)*0.5f+0.5f)*255), PApplet.parseInt((sin(PApplet.parseFloat(i)/30+3*PI/2)*0.5f+0.5f)*255)));
          conrl.setColorForeground(color(PApplet.parseInt((sin(PApplet.parseFloat(++i)/30+PI/2+PI)*0.5f+0.5f)*255), PApplet.parseInt((cos(PApplet.parseFloat(i)/15+PI+PI)*0.5f+0.5f)*255), PApplet.parseInt((sin(PApplet.parseFloat(i)/30+3*PI/2+PI)*0.5f+0.5f)*255)));
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

public void dropEvent(DropEvent evt)
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

public void loadOscFile()
{
  status.clear();
  cp5.getWindow().activateTab("deluxe");
  sequenceCreator();
  if (sequenceCreator.osc.unsaved) status.add("Warning! The current workspace is unsaved. Loading a file will overwrite without saving.");
  selectInput("Select an Oscillabstract workspace file (.osc)", "oscFileSelected");
}

public void oscFileSelected(File f)
{

  oscFileLoad(f);
}

public void loadInFile(File file)
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


public String getExtension(File file)
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

public void generateIcon()
{
  PGraphics p = createGraphics(256, 256);
  p.image(loadImage("Images/Icon2.png"), 0, 0);
  int o = p.get(50, 50);
  if (o != color(red(iconcolour), green(iconcolour), blue(iconcolour), alpha(o)) && generateicon)
  {
    int x1 = 75;
    int y1 = 65;
    int x2 = 181;
    int y2 = 195;
    float decay = 0.0004f;
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
        int c = color(red(iconcolour), green(iconcolour), blue(iconcolour), 255*exp(-sq(((float) j))*decay));
        p.set(i, y1+j, c);
      }
    }

    for (int i = y1; i < y2; i++)
    {
      for (int j = -150; j < 150; j++)
      {
        int c = color(red(iconcolour), green(iconcolour), blue(iconcolour), 255*exp(-sq(((float) j))*decay));
        int e = p.get((int) ((x1+x2)*0.5f+j), i);
        if (alpha(c) > alpha(e)) p.set((int) ((x1+x2)*0.5f+j), i, c);
      }
    }

    for (int i = x1; i < x2; i++)
    {
      for (int j = -150; j < 150; j++)
      {
        int c = color(red(iconcolour), green(iconcolour), blue(iconcolour), 255*exp(-sq(((float) j))*decay));
        int e = p.get(i, y2+j);
        if (alpha(c) > alpha(e)) p.set(i, y2+j, c);
      }
    }

    //The four corners:

    for (int i = -150; i < 0; i++)
    {
      for (int j = -150; j < 150; j++)
      {
        float rsq = sq(i)+sq(j);
        int c = color(red(iconcolour), green(iconcolour), blue(iconcolour), 255*exp(-(rsq)*decay));
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
    p.line((x1+x2)*0.5f, y1, (x1+x2)*0.5f, y2);
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

public void loadImagePaletteFile()
{

  status.clear();
  selectInput("Select an image file to load in as a palette", "paletteImageSelected");
}

public void paletteImageSelected(File f)
{
  if (f == null) 
  {
    status.add("Error: could not load palette file.");
    return;
  }
  if (paleditor == null) paleditor = new PalEditor();
  paletteSelected(f);
}

public void loadPicFile()
{
  status.clear();
  selectInput("Select a PIC file", "picFileSelected");
}

public void picFileSelected(File f)
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

public void loadCatFile()
{
  status.clear();
  selectInput("Select a CAT file", "catFileSelected");
}

public void catFileSelected(File f)
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

public void loadTextFile()
{
  status.clear();
  selectInput("Select a text file", "textFileSelected");
}

public void textFileSelected(File f)
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
            int c = 0;
            boolean blanked = false;
            int index = 0;
            float x = shrt ? map( PApplet.parseFloat(p[index++]), -32768, 32767, 0, width) : map( PApplet.parseFloat(p[index++]), -1, 1, 0, width) ;
            float y = shrt ? map( PApplet.parseFloat(p[index++]), -32768, 32767, height, 0) : map( PApplet.parseFloat(p[index++]), -1, 1, height, 0) ;
            float z = threedim ? (shrt ? map( PApplet.parseFloat(p[index++]), -32768, 32767, 0, depth) : map( PApplet.parseFloat(p[index++]), -1, 1, 0, depth) ) : depth*0.5f;
            pos.set(x, y, z);
            if ( PApplet.parseInt(p[index]) == -1) blanked = true;
            else
            {

              switch(colMod)
              {
              case 0 : 
                pal = PApplet.parseInt(p[index++]);
                c = 0;
                break;
              case 1 : 
                c = color(PApplet.parseInt(p[index++]), PApplet.parseInt(p[index++]), PApplet.parseInt(p[index++]));

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

public boolean inBetween(float value, float min, float max)
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
boolean about = false;

public void about()
{
  about = true;
}

public void exitAbout()
{
  about = false;
}

public void displayAbout()
{
  fill(laserboyMode ? color(PApplet.parseInt((sin(PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255)) : textcolour);
  textFont(f16);
  text("IldaViewer is written by colouredmirrorball.", 10, 50);
  text("It is open source and may be edited but not distributed (certainly not for profit!)", 10, 70);
  text("You are using version " + ildaViewerVersion + ".", 10, 90);
  String s = "It originated on ";
  text(s, 10, 110);
  String link = "Photonlexicon.com";
  fill(50, 75, 200);
  text(link, 10+textWidth(s), 110);
  if (mouseClicked && mouseOver(10+textWidth(s), 90, 10+textWidth(s)+textWidth(link), 110))
  {
    link("http://www.photonlexicon.com/forums/showthread.php/21601-Another-Ilda-view-tool");
  }

  fill(laserboyMode ? color(PApplet.parseInt((sin(0.5f+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(1.0f+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(0.5f+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255)) : textcolour);
  text("A detailed guide to using this program is in the Readme file.", 10, 140);
  text("There are also instructions on how to compile IldaViewer in Windows, OSX and Linux.", 10, 160);

  fill(laserboyMode ? color(PApplet.parseInt((sin(1.0f+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(2.0f+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(1.0f+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255)) : textcolour);
  text("If after reading the README and seeing the startup hints you still have questions,", 10, 190);
  text("or if you found a bug or have a suggestion, do not hesitate to leave a reply.", 10, 210);

  if (laserboyMode)
  {
    fill(50, 75, 200);
    String link2 = "This ";
    text(link2, 10, 240);
    fill(color(PApplet.parseInt((sin(1.5f+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(2.5f+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(1.5f+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255)) );
    text("is why it looks like an unicorn barfed over your screen.", 10+textWidth(link2), 240);
    if (mouseClicked && mouseOver(10, 220, 10+textWidth(link2), 240))
    {
      link("http://laserboy.org/");
    }
  }
}

/*
 * In this method all GUI elements are added.
 * For more information, see the ControlP5 documentary.
 * Callback methods for elements who have them are placed in the main tab.
 *
 * In short, you can add elements (buttons, toggles, lists, ...) to ControlP5. 
 * Each element has a name in the form of a String. In some cases, this can correspond
 * to a variable. For example, if you add a Toggle called "theToggle", and you have a
 * boolean theToggle in the program, the boolean will automatically get the value of the
 * Toggle that is being displayed on screen. (Reverse is not true, when some program element
 * changes the value of the boolean theToggle, the Toggle GUI button doesn't automatically
 * update. You need to manually set this by doing something like cp5.getController("theToggle").setValue(boolean(int(theToggle))); )
 * 
 * All GUI elements are grouped in Tabs. 
 */




public void initializeControlP5()
{

  //Mouseover help delay:
  cp5.getTooltip().setDelay(500);

  //Add all ControlP5 elements:

  //      === TAB: DEFAULT ===

  cp5.getTab("default")       //No need to initiate this tab
      .activateEvent(true)
      .setLabel("IldaViewer")
        .getCaptionLabel().setFont(f10);


  cp5.addButton("LoadIldaFile")
    .setPosition(10, 25)
      .moveTo("default")
        .setSize(100, 25)
          .setMoveable(true)
            .setLabel("Load an Ilda file")
              .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.getTooltip().register("LoadIldaFile", "Load an Ilda file and add it at the end of the existing frame list").getLabel().toUpperCase(false);

  importFile = cp5.addMultiList("importFile", 10, 55, 100, 25);
  importFile.setLabel("Import");
  MultiListButton b = importFile.add("Import", 1);
  b.add("Ilda", 2);
  b.add("Pic", 3);
  b.add("Cat", 4);
  b.add("Palette image", 5);
  b.add("Frame txt", 6);
  b.add("Oscillabstract", 7);

  cp5.addButton("clearFrames")
    .setPosition(width-130, 25)
      .setSize(120, 25)
        .moveTo("default")
          .setVisible(false)
            .setMoveable(true)
              .setLabel("Clear all frames")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.getTooltip().register("clearFrames", "Remove all frames").getLabel().toUpperCase(false);

  Group framePlayer = cp5.addGroup("framePlayer") //A group, for convenience
    .setPosition(width-240, 35)
      .setVisible(false)
        .moveTo("default")
          ;

  PImage[] previmgs = {
    loadImage("Images/previousframedefault.png"), loadImage("Images/previousframeactive.png"), loadImage("Images/previousframemouseover.png")
    };

    cp5.addBang("previousFrame")
      .setPosition(20, 5)
        .setImages(previmgs)
          .updateSize()
            .setTriggerEvent(Bang.RELEASE)
              .setLabel("<")
                .setGroup(framePlayer)
                  ;

  PImage[] nextimgs = {
    loadImage("Images/nextframedefault.png"), loadImage("Images/nextframeactive.png"), loadImage("Images/nextframemouseover.png")
    };

    cp5.addBang("nextFrame")
      .setPosition(60, 5)
        .setImages(nextimgs)
          .updateSize()
            .setGroup(framePlayer)
              .setTriggerEvent(Bang.RELEASE)
                .setLabel(">")
                  ;

  PImage[] firstimgs = {
    loadImage("Images/firstframedefault.png"), loadImage("Images/firstframeactive.png"), loadImage("Images/firstframemouseover.png")
    };

    cp5.addBang("firstFrame")
      .setPosition(0, 5)
        .setImages(firstimgs)
          .updateSize()
            .setGroup(framePlayer)
              .setTriggerEvent(Bang.RELEASE)
                .setLabel("<<")
                  ;

  PImage[] lastimgs = {
    loadImage("Images/lastframedefault.png"), loadImage("Images/lastframeactive.png"), loadImage("Images/lastframemouseover.png")
    };

    cp5.addBang("lastFrame")
      .setPosition(80, 5)
        .setImages(lastimgs)
          .updateSize()
            .setGroup(framePlayer)
              .setTriggerEvent(Bang.RELEASE)
                .setLabel(">>")
                  ;

  PImage[] playimgs = {
    loadImage("Images/playdefault.png"), loadImage("Images/playmouseover.png"), loadImage("Images/playactive.png")
    };

    cp5.addButton("autoPlay")      //This is supposed to be a Toggle, but images won't work for some reason
        .setPosition(40, 5)
        .setImages(playimgs)
          .setSwitch(true)
            .setGroup(framePlayer)
              .updateSize()
                .setLabel("Autoplay");



  cp5.addSlider("playbackSpeed")    //There is no callback method, as playback speed is stored in the variable playbackSpeed
    .setPosition(00, 30)
      .setSize(100, 20)
        .setRange(-60, 60)
          .setValue(12)
            .setGroup(framePlayer)
              .setValueLabel("Playback speed")
                ;

  cp5.getController("playbackSpeed").getCaptionLabel().setVisible(false);


  cp5.addBang("previousPalette")
    .setPosition(10, 85)
      .setSize(30, 30)
        .moveTo("default")
          .setTriggerEvent(Bang.RELEASE)
            .setLabel("< pal")
              .setVisible(false)
                ;

  cp5.getTooltip().register("previousPalette", "Recolour frame with previous palette in the palette list").getLabel().toUpperCase(false);

  cp5.addBang("nextPalette")
    .setPosition(42, 85)
      .setSize(30, 30)
        .moveTo("default")
          .setTriggerEvent(Bang.RELEASE)
            .setLabel("ette >")
              .setVisible(false)
                ;

  cp5.getTooltip().register("nextPalette", "Recolour frame with next palette in the palette list").getLabel().toUpperCase(false);

  cp5.addToggle("showBlanking")
    .setPosition(width-130, 60)
      .setSize(120, 25)
        .moveTo("default")
          .setVisible(false)
            .setMoveable(true)
              .setValueLabel("Show blanking lines")
                .setCaptionLabel("Show blanking")
                  .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.getTooltip().register("showBlanking", "Toggle the display of blanked lines").getLabel().toUpperCase(false);

  cp5.addToggle("showHeader")
    .setPosition(width-130, 106)
      .setSize(120, 12)
        .moveTo("default")
          .setVisible(true)
            .setMoveable(true)
              .setValueLabel("Show information")
                .setCaptionLabel("Show information")
                  .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.getTooltip().register("showHeader", "Show additional information about the program and frame").getLabel().toUpperCase(false);

  cp5.addToggle("laserboyMode")
    .setPosition(width-130, 90)
      .setSize(120, 12)
        .moveTo("default")
          .setValue(laserboyMode)
            .setVisible(true)
              .setMoveable(true)
                .setValueLabel("Show information")
                  .setCaptionLabel("Laserboy mode")
                    .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.getTooltip().register("laserboyMode", "And unicorns. Rainbows and unicorns.").getLabel().toUpperCase(false);

  //      === TAB: SEQUENCE EDITOR ===

  cp5.addTab("seqeditor")
    .activateEvent(true)
      .setLabel("Sequence Editor")
        .setId(1);



  cp5.addBang("insertFrame")
    .setPosition(width-300, 30)
      .setSize(85, 40)
        .moveTo("seqeditor")
          .setVisible(true)          
            .setMoveable(true)
              .setLabel("New frame")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addTextfield("nrOfInsertedFrames")
    .setPosition(width-300, 75)
      .setSize(85, 20)
        .setFocus(true)
          .setText(nrOfInsertedFrames)
            .moveTo("seqeditor")
              .setVisible(true)
                .setMoveable(true)
                  .setLabel("Amount")
                    .setAutoClear(false);


  cp5.addButton("deleteFrame")
    .setPosition(width-300, 125)
      .setSize(85, 85)
        .moveTo("seqeditor")
          .setVisible(true)
            .setMoveable(true)
              .setLabel("Delete frame")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addToggle("graphicView")
    .setPosition(width-300, 230)
      .setSize(85, 40)
        .moveTo("seqeditor")
          .setVisible(true)
            .setMoveable(true)
              .setLabel("Graphic view")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addButton("editorLoadIldaFile")
    .setPosition(width-300, 380)
      .moveTo("seqeditor")
        .setSize(85, 40)
          .setMoveable(true)
            .setLabel("Load Ilda file")
              .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addButton("revrs")
    .setPosition(width-300, 290)
      .moveTo("seqeditor")
        .setSize(85, 40)
          .setMoveable(true)
            .setLabel("Reverse")
              .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addButton("randomize")
    .setPosition(width-300, 335)
      .moveTo("seqeditor")
        .setSize(85, 40)
          .setMoveable(true)
            .setLabel("Randomize")
              .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addButton("editorClearFrames")
    .setPosition(width-300, 440)
      .setSize(85, 40)
        .moveTo("seqeditor")
          .setVisible(true)
            .setMoveable(true)
              .setLabel("Clear")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addButton("editorClearBuffer")
    .setPosition(width-300, 510)
      .setSize(85, 40)
        .moveTo("seqeditor")
          .setVisible(true)
            .setMoveable(true)
              .setLabel("Clear")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addButton("editorFitPalette")
    .setPosition(width-190, 30)
      .setSize(120, 40)
        .moveTo("seqeditor")
          .setVisible(true)
            .setMoveable(true)
              .setLabel("Fit RGB to palette")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);


  copyBehaviour = cp5.addRadioButton("copyBehaviour")
    .setPosition(width-190, 85)
      .moveTo("seqeditor")
        .setSize(25, 25)
          .setItemsPerRow(1)
            .setSpacingColumn(5)
              .setSpacingRow(4)
                .setNoneSelectedAllowed(false)
                  .addItem("Insert", 0)
                    .addItem("Overwrite", 1)
                      .addItem("Merge", 2)
                        .addItem("Copy frame data", 3)
                          .activate(0);


  copiedElements = cp5.addCheckBox("copiedElements")
    .setPosition(width-190, 210)
      .setSize(20, 20)
        .moveTo("seqeditor")
          .setVisible(false)
            .setItemsPerRow(2)
              .setSpacingColumn(75)
                .setSpacingRow(4)
                  .addItem("Frame header", 1)
                    .addItem("Point number", 1)
                      .addItem("X", 1)
                        .addItem("R", 1)
                          .addItem("Y", 1)
                            .addItem("G", 1)
                              .addItem("Z", 1)
                                .addItem("B", 1)
                                  .addItem("Blanking", 1)
                                    .addItem("Palette index", 1);

  cp5.addButton("editorSelectFrames")
    .setPosition(width-200, 440)
      .setSize(85, 40)
        .moveTo("seqeditor")
          .setVisible(true)
            .setMoveable(true)
              .setLabel("Select all")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addButton("editorSelectBuffer")
    .setPosition(width-200, 510)
      .setSize(85, 40)
        .moveTo("seqeditor")
          .setVisible(true)
            .setMoveable(true)
              .setLabel("Select all")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addButton("editorDeselectFrames")
    .setPosition(width-100, 440)
      .setSize(85, 40)
        .moveTo("seqeditor")
          .setVisible(true)
            .setMoveable(true)
              .setLabel("Deselect all")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addButton("editorDeselectBuffer")
    .setPosition(width-100, 510)
      .setSize(85, 40)
        .moveTo("seqeditor")
          .setVisible(true)
            .setMoveable(true)
              .setLabel("Deselect all")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);


  /*
  cp5.addToggle("editHeader")
   .setPosition(width-90, 88)
   .setSize(85, 10)
   .moveTo("seqeditor")
   .setVisible(true)
   .setMoveable(true)
   .setValueLabel("Edit")
   .setCaptionLabel("Edit")
   .getCaptionLabel().alignY(CENTER).alignX(CENTER);
   */

  //      === TAB: FRAME EDITOR ===

  /*
  cp5.addTab("framed")
   .activateEvent(true)
   .setLabel("Frame Editor")
   .setId(2);
   
   Group visibleControls = cp5.addGroup("visiblecontrols")
   .setPosition(10, 35)
   .setSize(120, 20)
   .setVisible(true)
   .setLabel("Control panels")
   .moveTo("framed");
   
   //This checkbox determines which GUI groups are visible
   //You need to program this in the setFrameEditorControls() method in the FrameEditor tab!
   
   ctrlsVis = new int[6];  //Total number of control groups
   
   controlsVisible = cp5.addCheckBox("visibleControls")
   .setPosition(0, 5)
   .setSize(15, 15)
   .setGroup(visibleControls)
   .setMoveable(true)
   .setItemsPerRow(1)
   .setSpacingColumn(17)
   .setSpacingRow(2)
   .addItem("Frame picker", 1)
   .addItem("Raster image", 10)
   .addItem("View Rotation", 100)
   .addItem("View Scale", 1000)
   .addItem("View position", 10000)
   .addItem("View Perspective", 100000);
   
   controlsVisible.activate(0);
   
   
   
   Group framePicker = cp5.addGroup("framePicker") //A group, for convenience
   .setPosition(150, 35)
   .setSize(120, 20)
   .setVisible(false)
   .moveTo("framed")
   ;
   
   cp5.addBang("previousFrameEd")
   .setPosition(20, 5)
   .setImages(previmgs)
   .updateSize()
   .setTriggerEvent(Bang.RELEASE)
   .setLabel("<")
   .setGroup(framePicker)
   ;
   
   cp5.addBang("nextFrameEd")
   .setPosition(80, 5)
   .setImages(nextimgs)
   .updateSize()
   .setGroup(framePicker)
   .setTriggerEvent(Bang.RELEASE)
   .setLabel(">")
   ;
   
   cp5.addBang("firstFrameEd")
   .setPosition(0, 5)
   .setImages(firstimgs)
   .updateSize()
   .setGroup(framePicker)
   .setTriggerEvent(Bang.RELEASE)
   .setLabel("<<")
   ;
   
   cp5.addBang("lastFrameEd")
   .setPosition(100, 5)
   .setImages(lastimgs)
   .updateSize()
   .setGroup(framePicker)
   .setTriggerEvent(Bang.RELEASE)
   .setLabel(">>")
   ;
   
   cp5.addToggle("listFrameEd")
   .setPosition(40, 5)
   .setSize(37, 20)
   .setGroup(framePicker)
   .setCaptionLabel("List")
   .getCaptionLabel().alignX(CENTER).alignY(CENTER)
   ;
   
   
   
   Group rasterImage = cp5.addGroup("rasterImage") //A group, for convenience
   .setPosition(300, 35)
   .setSize(120, 20)
   .setVisible(false)
   .setLabel("Raster image")
   .moveTo("framed")
   ;
   
   cp5.addButton("openFramedImage")
   .setPosition(0, 5)
   .setSize(57, 20)
   .setGroup(rasterImage)
   .setCaptionLabel("Open")
   .getCaptionLabel().alignX(CENTER);
   
   cp5.addToggle("hideFramedImage")
   .setPosition(63, 5)
   .setSize(57, 20)
   .setGroup(rasterImage)
   .setCaptionLabel("Hide")
   .getCaptionLabel().alignX(CENTER).alignY(CENTER);
   
   cp5.addButton("recolourFramedImage")
   .setPosition(0, 30)
   .setSize(120, 20)
   .setGroup(rasterImage)
   .setCaptionLabel("Colour to points")
   .getCaptionLabel().alignX(CENTER);
   
   cp5.addButton("positionFramedImage")
   .setPosition(0, 55)
   .setSize(57, 20)
   .setGroup(rasterImage)
   .setCaptionLabel("Position")
   .getCaptionLabel().alignX(CENTER);
   
   cp5.addToggle("sizeFramedImage")
   .setPosition(63, 55)
   .setSize(57, 20)
   .setGroup(rasterImage)
   .setCaptionLabel("Size")
   .getCaptionLabel().alignX(CENTER).alignY(CENTER);
   
   cp5.addSlider("sizeXYFramedImage")
   .setPosition(0, 80)
   .setSize(120, 20)
   .setGroup(rasterImage)
   .setRange(-1.5, 1.5)
   .setValue(1)
   .setVisible(false)
   .setSliderMode(Slider.FLEXIBLE)
   .setCaptionLabel("Size")
   .getCaptionLabel().alignX(CENTER)
   ;
   
   
   cp5.addSlider("sizeXFramedImage")
   .setPosition(0, 105)
   .setSize(120, 20)
   .setGroup(rasterImage)
   .setRange(-1.5, 1.5)
   .setValue(1)
   .setVisible(false)
   .setSliderMode(Slider.FLEXIBLE)
   .setCaptionLabel("Size X")
   .getCaptionLabel().alignX(CENTER)
   ;
   
   cp5.addSlider("sizeYFramedImage")
   .setPosition(0, 130)
   .setSize(120, 20)
   .setGroup(rasterImage)
   .setRange(-1.5, 1.5)
   .setValue(1)
   .setVisible(false)
   .setSliderMode(Slider.FLEXIBLE)
   .setCaptionLabel("Size Y")
   .getCaptionLabel().alignX(CENTER)
   ;
   
   
   
   
   Group rotationControl = cp5.addGroup("rotationControl")
   .setPosition(450, 35)
   .setSize(120, 20)
   .setVisible(false)
   .setLabel("View Rotation")
   .moveTo("framed")
   ;
   
   cp5.addSlider("rotationX")
   .setPosition(0, 5)
   .setSize(120, 20)
   .setGroup(rotationControl)
   .setRange(-1, 1)
   .setSliderMode(Slider.FLEXIBLE)
   .setCaptionLabel("X")
   .getCaptionLabel().alignX(CENTER)
   ;
   
   cp5.addSlider("rotationY")
   .setPosition(0, 30)
   .setSize(120, 20)
   .setGroup(rotationControl)
   .setRange(-1, 1)
   .setSliderMode(Slider.FLEXIBLE)
   .setCaptionLabel("Y")
   .getCaptionLabel().alignX(CENTER)
   ;
   
   cp5.addSlider("rotationZ")
   .setPosition(0, 55)
   .setSize(120, 20)
   .setGroup(rotationControl)
   .setRange(-1, 1)
   .setSliderMode(Slider.FLEXIBLE)
   .setCaptionLabel("Z")
   .getCaptionLabel().alignX(CENTER)
   ;
   
   Group sizeControl = cp5.addGroup("sizeControl")
   .setPosition(1, 1)
   .setSize(120, 20)
   .setVisible(false)
   .setLabel("View Scale")
   .moveTo("framed")
   ;
   
   cp5.addSlider("zoom")
   .setPosition(0, 5)
   .setSize(120, 20)
   .setGroup(sizeControl)
   .setRange(-0.1, 0.1)
   .setValue(0)
   .setVisible(true)
   .setSliderMode(Slider.FLEXIBLE)
   .setCaptionLabel("Zoom")
   .getCaptionLabel().alignX(CENTER)
   ;
   
   
   cp5.addButton("resetZoom")
   .setPosition(0, 30)
   .setSize(120, 20)
   .setGroup(sizeControl)
   .setCaptionLabel("Reset")
   .getCaptionLabel().alignX(CENTER);
   
   Group positionControl = cp5.addGroup("positionControl")
   .setPosition(1, 1)
   .setSize(120, 20)
   .setVisible(false)
   .setLabel("View Position")
   .moveTo("framed")
   ;
   
   Group perspective = cp5.addGroup("perspective")
   .setPosition(1, 1)
   .setSize(120, 20)
   .setVisible(false)
   .setLabel("View Perspective")
   .moveTo("framed")
   ;
   
   cp5.addSlider("viewPerspective")
   .setPosition(0, 5)
   .setSize(120, 20)
   .setGroup(perspective)
   .setRange(0, 10)
   .setValue(0)
   .setVisible(true)
   .setSliderMode(Slider.FLEXIBLE)
   .setCaptionLabel("Perspective")
   .getCaptionLabel().alignX(CENTER)
   ;
   
   */

  //      === TAB: PALETTES ===

  cp5.addTab("palettes")
    .activateEvent(true)
      .setLabel("Palette editor")
        .setId(4);

  paletteList = cp5.addListBox("myList")
    .setPosition(10, 150)
      .setSize(145, 300)
        .moveTo("palettes")
          .setLabel("All palettes")
            .setItemHeight(15)
              .setBarHeight(15)
                ;

  paletteList.captionLabel().toUpperCase(true);
  paletteList.captionLabel().style().marginTop = 3;
  paletteList.toUpperCase(false);
  paletteList.valueLabel().style().marginTop = 3;

  cp = cp5.addColorPicker("picker")
    .setPosition(width-425, 450)
      .moveTo("palettes")
        ;

  cp5.addButton("recolourFrames")
    .setPosition(10, 25)
      .setSize(145, 25)
        .moveTo("palettes")
          .setCaptionLabel(" Recolour frames")
            .setVisible(true)
              .getCaptionLabel().alignX(CENTER);


  cp5.addButton("addPalette")
    .setPosition(10, 60)
      .setSize(70, 25)
        .moveTo("palettes")
          .setCaptionLabel("New")
            .setVisible(true)
              .getCaptionLabel().alignX(CENTER);

  cp5.addButton("removePalette")
    .setPosition(85, 60)
      .setSize(70, 25)
        .moveTo("palettes")
          .setCaptionLabel("Delete")
            .setVisible(true)
              .getCaptionLabel().alignX(CENTER);

  cp5.addButton("importPalette")
    .setPosition(10, 90)
      .setSize(70, 25)
        .moveTo("palettes")
          .setCaptionLabel("Import")
            .setVisible(true)
              .getCaptionLabel().alignX(CENTER);

  cp5.addButton("exportPalette")
    .setPosition(85, 90)
      .setSize(70, 25)
        .moveTo("palettes")
          .setCaptionLabel("Export")
            .setVisible(true)
              .getCaptionLabel().alignX(CENTER);

  PFont font = createFont("arial", 12);

  cp5.addTextfield("input")
    .setPosition(width-150, 490)
      .setSize(120, 20)
        .setFont(font)
          .moveTo("palettes")
            .setCaptionLabel("Rename")
              .setFocus(true)
                // .setColor(color(255, 0, 0))
                ;

  cp5.addTextfield("numberOfColours")
    .setPosition(width-425, 515)
      .setSize(120, 20)
        .setFont(font)
          .moveTo("palettes")
            .setCaptionLabel("Number of colours (< 256)")
              .setFocus(true)
                // .setColor(color(255, 0, 0))
                ;




  //      === TAB: SEQUENCE CREATOR ===



  cp5.addTab("deluxe")        //Initiating the tab to go to the Deluxe Paint mode
    .activateEvent(true)
      .setLabel("Oscillabstract")
        .setId(5)
          //.setColorBackground(color(0, 0, 100))
          //  .setColorLabel(color(255))
          //    .setColorActive(color(0, 150, 255))
          ;
  /*
  Group seqControlsVisible = cp5.addGroup("seqControlsVisible")
   .setPosition(10, 65)
   .setSize(150, 10)
   .setVisible(true)
   .setLabel("Select a sequence mode")
   .moveTo("deluxe");
   
   seqMode = cp5.addRadioButton("seqMode")
   .setGroup(seqControlsVisible)
   .setSize(150, 25)
   .setPosition(0, 1)
   .setItemsPerRow(1)
   .setSpacingRow(1)
   .addItem("Deluxe Paint", 0)
   .addItem("Oscillabstract", 1);
   
   for (Toggle t : seqMode.getItems ())
   {
   t.getCaptionLabel().alignY(CENTER).alignX(CENTER);
   }
   */
  cp5.addButton("clearSeqCreatorFrames")
    .setPosition(width-90, 25)
      .setSize(85, 25)
        .moveTo("deluxe")
          .setMoveable(true)
            .setLabel("Clear")
              .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.getTooltip().register("clearDeluxeFrames", "Reset the current sequence").getLabel().toUpperCase(false);

  cp5.addButton("finishSeqCreator")
    .setPosition(10, 25)
      .setSize(150, 25)
        .moveTo("deluxe")      //Only visible in Deluxe Paint tab
          .setMoveable(true)
            .setLabel("Import in IldaViewer")
              .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.getTooltip().register("finishDeluxePaint", "Import sequence to frame list").getLabel().toUpperCase(false);

  cp5.addToggle("showSQBlanking")
    .setPosition(width-90, 55)
      .setSize(85, 25)
        .moveTo("deluxe")
          .setValue(showBlanking)
            .setMoveable(true)
              .setValueLabel("Show blanking lines")
                .setCaptionLabel("Show blanking")
                  .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addButton("emptyOscElements")
    .setPosition(width-90, 25)
      .setSize(85, 25)
        .moveTo("deluxe")
          .setMoveable(true)
            .setVisible(false)
              .setLabel("Clear")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addButton("loadElements")
    .setPosition(width-90, 55)
      .setSize(85, 25)
        .moveTo("deluxe")
          .setMoveable(true)
            .setVisible(false)
              .setLabel("Load")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addButton("saveElements")
    .setPosition(width-90, 85)
      .setSize(85, 25)
        .moveTo("deluxe")
          .setMoveable(true)
            .setVisible(false)
              .setLabel("Save")
                .getCaptionLabel().alignY(CENTER).alignX(CENTER);



  //      === TAB: EXPORT ===

  cp5.addTab("export")
    .activateEvent(true)
      .setLabel("Export to file")
        .setId(4);

  cp5.addButton("exportFile")
    .setPosition(10, 25)
      .setSize(100, 100)
        .moveTo("export")      //Only visible in Deluxe Paint tab
          .setMoveable(true)
            .setLabel("Export file")
              .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  r = cp5.addRadioButton("radioButton")
    .setPosition(135, 25)
      .moveTo("export")
        .setSize(22, 22)
          .setItemsPerRow(1)
            .setSpacingColumn(5)
              .setSpacingRow(4)
                .setNoneSelectedAllowed(false)
                  .addItem("Ilda format 0: 3D, palette", 0)
                    .addItem("Ilda format 1: 2D, palette", 1)
                      .addItem("Ilda format 4: 3D, BGR (true colour)", 4)
                        .addItem("Ilda format 5: 2D, BGR (true colour)", 5)
                          .addItem("PIC: single frame, RGB/palette combination", 6)
                            .addItem("CAT: old format (palette only)", 7)
                              .addItem("CAT: new format (palette/RGB combination)", 8)
                                .activate(exportVersion > 3 ? exportVersion - 2 : exportVersion);

  cp5.addToggle("exportWholeFile")
    .setPosition(10, 135)
      .setSize(100, 25)
        .moveTo("export")
          .setValue(exportWholeFile)
            .setMoveable(true)
              .setValueLabel("Show blanking lines")
                .setCaptionLabel("All frames")
                  .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  exportingFrames = cp5.addTextlabel("labelExportingFrames")
    .setText("Exporting from to")
      .setPosition(7, 165)
        .moveTo("export")
          .setVisible(false)
            .setFont(font)
              ;

  cp5.addButton("optSettings")
    .setPosition(width-185, 55)
      .setSize(160, 25)
        .moveTo("export")
          .setLabel("Optimisation settings")
            .setVisible(optimise)
              .getCaptionLabel().alignY(CENTER).alignX(CENTER);
/*
  cp5.addToggle("optimise")
    .setPosition(width-185, 25)
      .setSize(160, 25)
        .setVisible(true)
          .moveTo("export")
            .setValue(optimise)
              .setMoveable(true)
                .setCaptionLabel("Optimise")
                  .getCaptionLabel().alignY(CENTER).alignX(CENTER);
*/


  cp5.addButton("previousPaletteExp")
    .setPosition(width-185, 185)
      .setSize(75, 25)
        .moveTo("export")
          .setLabel("< Previous")
            .setVisible(false)
              ;

  cp5.addButton("nextPaletteExp")
    .setPosition(width-100, 185)
      .setSize(75, 25)
        .moveTo("export")
          .setLabel("Next >")
            .setVisible(false)
              .getCaptionLabel().alignX(RIGHT)
                ;

  cp5.addToggle("findBestColour")
    .setPosition(width-185, 155)
      .setSize(160, 25)
        .setVisible(false)
          .moveTo("export")
            .setValue(includePalette)
              .setMoveable(true)
                .setCaptionLabel("Fit colours")
                  .getCaptionLabel().alignY(CENTER).alignX(CENTER);

  cp5.addToggle("includePalette")
    .setPosition(width-185, 125)
      .setSize(160, 25)
        .setVisible(false)
          .moveTo("export")
            .setValue(includePalette)
              .setMoveable(true)
                .setCaptionLabel("Include palette")
                  .getCaptionLabel().alignY(CENTER).alignX(CENTER);




  cp5.addTab("about")
    .activateEvent(true)
      .setLabel("About")
        .setId(5);

  /*
    
   //Pro laserist mode no longer supported. Complaints can be sent to the nearest mirror.
   
   //Placed after all Controllers, otherwise nullpointererrors galore
   cp5.addToggle("proLaseristMode")
   .setPosition(width-90, 90)
   .setSize(85, 10)
   .moveTo("default")
   .setValue(proLaseristMode)
   .setVisible(true)
   .setMoveable(true)
   .setValueLabel("Show information")
   .setCaptionLabel("Pro Laserist Mode")
   .getCaptionLabel().alignY(CENTER).alignX(CENTER);
   */
}


// Fields for Export tab:

RadioButton r;                //ControlP5 radio button (export file format selection)
int exportVersion = 4;        //Create Ilda V4 file by default
FileExport exporter;
boolean exporting = false;    //Exporting mode
Textlabel exportingFrames;    //Some ControlP5 text
boolean includePalette = true;//Should the palette be included in the file?
boolean exportWholeFile = true;//Export all frames by default
boolean findBestColour = true; //RGB points get automatically a fitting palette index
boolean optimise = false;

//      === EXPORT TAB METHODS AND CALLBACKS ===

public void beginExporter()
{
  exporting = true;
  if (exporter == null) exporter = new FileExport();

  if (includePalette && isPaletteFile())
  {
    cp5.getController("nextPaletteExp").setVisible(true);
    cp5.getController("previousPaletteExp").setVisible(true);
    if (exportVersion == 0 || exportVersion == 1) cp5.getController("findBestColour").setVisible(true);
  } else
  {
    cp5.getController("nextPaletteExp").setVisible(false);
    cp5.getController("previousPaletteExp").setVisible(false);
    cp5.getController("findBestColour").setVisible(false);
  }

  status.clear();
  status.add("Choose your options then click the big button.");
}

public void endExporter()
{
  exporting = false;
  status.clear();
  status.add("Exporter exited");
}

public void exportFile()
{
  String pathname;
  String extension = getExtension();
  if (frames.isEmpty()) pathname = extension;
  else pathname = frames.get(frames.size()-1).frameName + extension;
  File theFile = new File(pathname);
  status.clear();
  status.add("Select where to write a file");
  selectOutput("Choose an output location", "ildaOutput", theFile);
}

public String getExtension()
{
  String extension = "";
  if (exportVersion == 0 || exportVersion == 1 || exportVersion == 4 || exportVersion == 5) extension = ".ild";
  if (exportVersion == 6) extension = ".pic";
  if (exportVersion == 7 || exportVersion == 8) extension = ".cat";
  return extension;
}

public boolean isPaletteFile()
{
  return exportVersion == 0 || exportVersion == 1 || exportVersion == 6 || exportVersion == 7 || exportVersion == 8;
}

public void ildaOutput(File selection)
{
  if (selection == null)
  {
    status.clear();
    status.add("File exporting aborted.");
    return;
  }
  status.clear();

  String location = selection.getAbsolutePath();
  char[] test = new char[4];  //Test if it already has the extension .ild:
  for (int i = 0; i < 4; i++)
  {
    test[i] = location.charAt(i+location.length()-4);
  }
  String testing = new String(test);
  String extension = getExtension();
  if ( !testing.equals(extension) )
  {
    location += extension;
  }

  if (exportVersion == 6) exporter.lastFrame = exporter.firstFrame;

  ArrayList<Frame> expframes = new ArrayList<Frame>();

  // You found me! Cheater!
  if (frames.isEmpty() )
  {
    Frame easterEggFrame = new Frame();
    float redPhase = random(0, TWO_PI);      //Colour modulation parameters
    float greenPhase = random(0, TWO_PI);
    float bluePhase = random(0, TWO_PI);
    int xFreq = PApplet.parseInt(random(1, 15));
    int yFreq = PApplet.parseInt(random(1, 15));
    int zFreq = PApplet.parseInt(random(1, 3));
    int redFreq = PApplet.parseInt(random(1, xFreq*zFreq));
    int greenFreq = PApplet.parseInt(random(1, yFreq*zFreq));
    int blueFreq = PApplet.parseInt(random(1, xFreq*yFreq/(5*zFreq)));
    int maxPoints = 499;
    for (int i = 0; i <= maxPoints; i++)
    {
      PVector position = new PVector(
      width*(sin(PApplet.parseFloat(i)*TWO_PI*PApplet.parseFloat(xFreq)/maxPoints)*cos(PApplet.parseFloat(i)*TWO_PI*PApplet.parseFloat(yFreq)/maxPoints)*0.35f+0.5f), 
      height*(sin(PApplet.parseFloat(i)*TWO_PI*PApplet.parseFloat(xFreq)/maxPoints)*sin(PApplet.parseFloat(i)*TWO_PI*PApplet.parseFloat(yFreq)/maxPoints)*0.35f+0.5f), 
      depth*(sin(PApplet.parseFloat(i)*TWO_PI*PApplet.parseFloat(xFreq*zFreq)/maxPoints)*cos(PApplet.parseFloat(i)*TWO_PI*2*PApplet.parseFloat(yFreq*zFreq)/maxPoints)*0.35f+0.5f)
        );

      Point thepoint = new Point(position, PApplet.parseInt((sin(PApplet.parseFloat(i)*TWO_PI*PApplet.parseFloat(redFreq)/maxPoints+redPhase)*0.5f+0.5f)*255), PApplet.parseInt((sin(PApplet.parseFloat(i)*TWO_PI*PApplet.parseFloat(greenFreq)/maxPoints+greenPhase)*0.5f+0.5f)*255), PApplet.parseInt((sin(PApplet.parseFloat(i)*TWO_PI*PApplet.parseFloat(blueFreq)/maxPoints+bluePhase)*0.5f+0.5f)*255), false);
      easterEggFrame.points.add(thepoint);
    }
    easterEggFrame.ildaVersion = 4;
    easterEggFrame.frameName = "EastrEgg";
    easterEggFrame.companyName = "IldaView";
    easterEggFrame.pointCount = 500;
    easterEggFrame.frameNumber = 1;
    easterEggFrame.totalFrames = 1;
    easterEggFrame.scannerHead = 9001;
    easterEggFrame.fitColourIndexWithPalette(getActivePalette());
    expframes.add(easterEggFrame);
    status.add("Laid an egg.");
  } else
  {
    //Only include selected frames:
    if (exporter.firstFrame >= 0 && exporter.lastFrame < frames.size())
    {
      if (exportWholeFile) expframes = frames;
      else
      {
        for (int i = exporter.firstFrame; i <= exporter.lastFrame; i++)
        {
          expframes.add(frames.get(i));
        }
      }
    }
  }

  if (findBestColour)
  {
    for (Frame frame : expframes)
    {
      frame.fitColourIndexWithPalette(getActivePalette());
    }
  }

  ArrayList<Frame> optFrames = new ArrayList<Frame>(expframes.size());
  if (optimise)
  {
    for (int i = 0; i < expframes.size (); i++)
    {
      Frame frame = new Frame(expframes.get(i).clone());
      Frame frame2 = new Frame();

      for (Point p : frame.points)
      {
        frame2.points.add(new Point(p.clone(p)));
      }

      frame2 = frame2.writeProperties(frame, frame2);

      exporter.optimizer.setFrame(frame2);
      exporter.optimizer.optimise();
      frame2 = exporter.optimizer.getFrame();

      optFrames.add(frame2);
    }
  } else optFrames = expframes;

  if (includePalette && (exportVersion == 0 || exportVersion == 1)) 
  {
    FileWrapper theFile = new FileWrapper(location, optFrames, getActivePalette(), exportVersion);
  } else 
  {
    FileWrapper theFile = new FileWrapper(location, optFrames, exportVersion);
  }

  status.add("File exported without errors to location:");
  status.add(location);
}


public void exportWholeFile(boolean shouldIt)
{
  if (cp5.getWindow().getCurrentTab().getName().equals("export"))
  {
    if (shouldIt  )
    {
      exporter.firstFrame = 0;
      exporter.lastFrame = frames.size()-1;
      if (frames.isEmpty() ) exporter.lastFrame = 0;

      cp5.getController("exportWholeFile").setCaptionLabel("All frames");
      exportingFrames.setVisible(false);
    } else
    {
      exporter.displayFrames();
      exportingFrames.setVisible(true);
      exportingFrames.setText("Exporting from " + (exporter.firstFrame+1) + " to " + (exporter.lastFrame+1));
      cp5.getController("exportWholeFile").setCaptionLabel("Selected frames");
      status.clear();
      status.add("Left click on the first frame, right click on the last frame. Drag with middle mouse button to scroll.");
    }
    exporter.exportAllFrames = shouldIt;
  }
  exportWholeFile = shouldIt;
}



public void includePalette(boolean shouldWe)
{
  includePalette = shouldWe;
  if (shouldWe)
  {
    cp5.getController("nextPaletteExp").setVisible(true);
    cp5.getController("previousPaletteExp").setVisible(true);
    cp5.getController("findBestColour").setVisible(true);
    cp5.getController("includePalette").getCaptionLabel().setText("Include palette");
  } else
  {
    cp5.getController("nextPaletteExp").setVisible(false);
    cp5.getController("previousPaletteExp").setVisible(false);
    cp5.getController("findBestColour").setVisible(false);
    cp5.getController("includePalette").getCaptionLabel().setText("Don't include palette");
  }
}

public void previousPaletteExp() //manually skip to previous active palette
{
  if (activePalette <= 0)
  {
    activePalette = palettes.size()-1;
  } else 
  {
    activePalette--;
  }
}

public void nextPaletteExp() //manually skip to next active palette
{
  if (activePalette >= palettes.size()-1)
  {
    activePalette = 0;
  } else
  {
    activePalette++;
  }
}

public void findBestColour(boolean doIttt)
{
  if (doIttt) cp5.getController("findBestColour").getCaptionLabel().setText("Fit colours");
  else cp5.getController("findBestColour").getCaptionLabel().setText("Don't fit colours");
}

public void optimise(boolean o)
{
  cp5.getController("optSettings").setVisible(o);
}

public void optSettings()
{
  if (!exporter.optimizer.guiVisible)
  {
    OptimizerOverlay oo = new OptimizerOverlay(exporter.optimizer, 50, 50, 480, 320);
    PFrame f = new PFrame(oo, 480, 320);
  }
}



class FileExport
{
  int firstFrame = 0;
  int lastFrame = frames.size()-1;
  boolean exportAllFrames = true;
  PGraphics framelist;
  PGraphics overlay;
  int framesSizeX = 100;
  int frameSize = framesSizeX-10;
  int frameListSizeY = height - 190;
  int yoffset = 0;
  int framesYPos = 180;
  int prevYOffset = -1;
  int visibleFrames = frameListSizeY/(framesSizeX-10);
  boolean scrolling = false;       //scrolling with middle mouse button
  boolean startScrolling = false;  //scrolling with scrollbar
  Optimizer optimizer;
  boolean showOptSettings = false;

  FileExport()
  {


    optimizer = new Optimizer();
  }

  public void update()
  {
    if (showOptSettings)
    {
      //optimizer.update();
      //optimizer.display();
    }



    if (includePalette && (exportVersion == 0 || exportVersion == 1) || exportVersion == 6 || exportVersion == 7 || exportVersion == 8)
    {
      if (activePalette >= 0  || activePalette < palettes.size())
      {
        displayPalette(palettes.get(activePalette));
      }
      fill(255);
      text(palettes.get(activePalette).name, width-185, 395);
    }

    if (!exportAllFrames)
    {
      if (yoffset != prevYOffset)
      {

        framelist.beginDraw();
        framelist.fill(50);
        framelist.noStroke();
        framelist.rect(0, 0, framesSizeX-1, framelist.height-1);

        if (!frames.isEmpty())
        {
          //println(max ( (int) -yoffset/frameSize-2, 0), min(frames.size(), (int) -yoffset/frameSize+visibleFrames+1));
          for (int i = max ( (int) -yoffset/(frameSize+5)-1, 0); i < min(frames.size(), (int) -yoffset/(frameSize+5)+visibleFrames+2); i++)
          {
            framelist.noStroke();

            if (i == firstFrame)
            {
              framelist.strokeWeight(3);
              framelist.stroke(255, 255, 0);
            }

            if (i == lastFrame && exportVersion != 6)
            {
              framelist.strokeWeight(3);
              framelist.stroke(0, 255, 255);
            }

            if ( i< lastFrame && i > firstFrame && exportVersion != 6)
            {
              framelist.strokeWeight(2); 
              framelist.stroke(lerp(0, 255, 1-((float)i-(float)firstFrame)/(lastFrame)), 255, lerp(0, 255, ((float)i-(float)firstFrame)/(lastFrame)));
            }

            framelist.fill(0);
            framelist.rect(5, yoffset+(frameSize+5)*i, 90, 90);
            frames.get(i).drawFrame(framelist, 5, (float)yoffset+(frameSize+5)*i, 0.0f, 90.0f, 90.0f, 0.0f, false, true);
            if (i == firstFrame)
            {
              framelist.fill(255, 50);
              framelist.textFont(f16);
              framelist.text("First", 7, yoffset+(frameSize+5)*i+15);
            }
            if (i == lastFrame)
            {
              framelist.fill(255, 50);
              framelist.textFont(f16);
              framelist.text("Last", 7, yoffset+(frameSize+5)*i+75);
            }
          }
        } else
        {
          framelist.textFont(f16);
          framelist.fill(255, 50);
          framelist.text("No frames", 5, 55);
        }
        framelist.endDraw();
      }

      firstFrame = constrain(firstFrame, 0, frames.size()-1);
      lastFrame = constrain(lastFrame, 0, frames.size()-1);

      imageMode(CORNER);

      image(framelist, 10, framesYPos);
      image(overlay, 10, framesYPos);


      if (mousePressed && mouseX > 10 && mouseX < 10+framesSizeX && mouseY > framesYPos)
      {
        int chosenFrame = (int) (mouseY-yoffset-framesYPos)/(frameSize+5);
        if (mouseButton == LEFT) 
        {
          firstFrame = min(max(0, chosenFrame), frames.size()-1);
          exportingFrames.setText("From " + (exporter.firstFrame+1) + " to " + (exporter.lastFrame+1));
        }
        if (mouseButton == CENTER && !scrolling) 
        {
          scrolling = true;
          prevYOffset = -yoffset + mouseY;
          cursor(MOVE);
        }
        if (mouseButton == RIGHT) 
        {
          lastFrame = min(max(0, chosenFrame), frames.size()-1);
          exportingFrames.setText("From " + (exporter.firstFrame+1) + " to " + (exporter.lastFrame+1));
        }
      }

      if (!mousePressed && scrolling)
      {
        scrolling = false;
        cursor(ARROW);
      }

      if (scrolling)
      {
        yoffset = mouseY - prevYOffset;
      }

      float scrollY = map(yoffset, 0, -frames.size() * (frameSize+5) + (frameSize+5) * visibleFrames, framesYPos+45, height-70);

      fill(127);
      noStroke();
      rect(113, framesYPos, 20, 20);
      rect(113, framesYPos+22, 20, 20);
      rect(113, height-60, 20, 20);
      rect(113, height-38, 20, 20);
      rect(113, scrollY, 20, 7);
      fill(0);
      text("^", 119, framesYPos+15);
      text("^", 119, framesYPos+21);
      text("^", 119, framesYPos+40);
      text("v", 119, height-45);
      text("v", 119, height-26);
      text("v", 119, height-20);

      if (mousePressed && mouseX > 115 && mouseX < 135)
      {
        if (inBetween(mouseY, framesYPos, framesYPos+20)) yoffset = 0;
        if (inBetween(mouseY, framesYPos+22, framesYPos+42)) yoffset+=5;
        if (inBetween(mouseY, height-60, height-40)) yoffset-=5;
        if (inBetween(mouseY, height-38, height-18)) yoffset = -frames.size() * (frameSize+5) + (frameSize+5) * visibleFrames;
        if (inBetween(mouseY, scrollY, scrollY+7)) startScrolling = true;
      }

      if (startScrolling)
      {
        yoffset = (int) map(mouseY, framesYPos+45, height-70, 0, -frames.size() * (frameSize+5) + (frameSize+5) * visibleFrames);
      }

      if (frames.size() > visibleFrames) yoffset = constrain(yoffset, -frames.size() * (frameSize+5) + (frameSize+5) * visibleFrames, 0);
      else yoffset = 0;

      if (startScrolling && !mousePressed) startScrolling = false;
    }
  }

  public void displayFrames()
  {
    framelist = createGraphics(100, frameListSizeY, P3D);
    overlay = createGraphics(100, frameListSizeY);
    overlay.beginDraw();
    for (int i = 0; i < 30; i++)
    {
      overlay.stroke(red(backgroundcolour), green(backgroundcolour), blue(backgroundcolour), 255-i*8.5f);
      overlay.line(0, i, framesSizeX, i);
      overlay.line(0, frameListSizeY-i, framesSizeX, frameListSizeY-i);
    }
    overlay.endDraw();
  }

  public void displayPalette(Palette palette)
  {
    int i = 0;
    for (PaletteColour colour : palette.colours)
    {
      stroke( color(50, 50, 50));
      strokeWeight(1);

      colour.displayColour(width-185+10*(i%16), 215+10*PApplet.parseInt(i/16), 10);
      i++;
    }
  }
}

/*
 * This class reads a file and passes the data to frames and points. 
 * It's advised to not store instances of this class if not necessary, as it can get quite voluminous
 * after loading in large files. 
 * 
 * Ilda files are explained here: http://www.laserist.org/StandardsDocs/IDTF05-finaldraft.pdf
 * This document only mentions Ilda V0, 1, 2 and 3, no V4 and V5 so here's a breakdown:
 * ILDA V0 is 3D and uses palettes
 * ILDA V1 is 2D and uses palettes
 * ILDA V2 is a palette
 * ILDA V3 is a 24-bit palette, but was discontinued and is not a part of the official standard anymore
 * ILDA V4 is 3D with true-colour information in BGR format
 * ILDA V5 is 2D with true-colour information in BGR format
 *
 * An Ilda file is composed of headers that always start with "ILDA", followed by three zeros and the version number.
 * A complete header is 32 bytes.
 * After the header, the data follows. In case of a palette (V2), each data point has three bytes: R, G and B.
 * In case of a frame (V0/1/4/5), the X, Y and Z (for 3D frames) values are spread out over two bytes
 * They can be joined together with the unsignedShortToInt() method heroic graciously made in the PL IRC channel.
 * Then either two status bytes follow with a blanking bit and palette colour number, or BGR values.
 * 
 */


class FileWrapper
{
  String location;         //Absolute path to the file
  byte[] b;                //Array with bits readed from the file
  IntList framePositions;  //IntList with indices in b[] where a new ILDA header has been found
  String name = "";

  FileWrapper(String _location)    //Constructor, automatically reads file
  {
    location = _location;
    try { 
      b = loadBytes(location);
      readFile();
    }
    catch(Exception e)
    {
      status.add("Could not read file");
      if (e.getMessage() != null) status.add(e.getMessage());
    }
  }

  FileWrapper(String _location, ArrayList<Frame> frames, Palette _palette, int ildVersion)    //Constructor, automatically writes file with provided palette
  {

    location = _location;
    b = framesToBytes(frames, ildVersion);
    writeFile(_palette);
  }

  FileWrapper(String _location, ArrayList<Frame> dframes, int ildVersion)    //Constructor, automatically writes file
  {

    location = _location;
    b = framesToBytes(dframes, ildVersion);
    writeFile();
  }

  FileWrapper(String _location, boolean notEvenUsedBecauseISuckAtProgramming)
  {
    b = loadBytes(_location);
  }

  FileWrapper(byte[] b)
  {
    this.b = b;
  }

  //Cascade of writeFiles():
  public void writeFile(Palette _palette)
  {
    writeFile(location, b, _palette);
  }

  public void writeFile()
  {
    writeFile(location, b);
  }

  //Merge a palette table with an existing array of bytes (the palette is placed at the beginning of the file)
  public void writeFile(String location, byte[] _b, Palette aPalette)
  {
    byte[] pbytes = aPalette.paletteToBytes();
    /*
    int totalLength = _b.length + pbytes.length;
     byte[] merged = new byte[totalLength];
     for (int i = 0; i < pbytes.length; i++)
     {
     merged[i] = pbytes[i];
     }
     for (int i = 0; i < _b.length; i++)
     {
     merged[i+pbytes.length] = _b[i];
     }
     */
    byte[] merged = concat(pbytes, _b);

    writeFile(location, merged);
  }

  //Ah finally, a writeFile() that actually writes to a file!
  public void writeFile(String location, byte[] _b)
  {
    if (_b != null) saveBytes(location, _b);
  }

  //Convert an ArrayList of Frames to ilda-compliant bytes:
  public byte[] framesToBytes(ArrayList<Frame> frames, int formatNumber)
  {
    if (frames.isEmpty()) return null;
    if (formatNumber == 0 || formatNumber == 1 || formatNumber == 4 || formatNumber == 5) return framesToIldaFile(frames, formatNumber);
    if (formatNumber == 6) return frameToPicFile(frames.get(0));
    if (formatNumber == 7) return framesToOldCatFile(frames);
    if (formatNumber == 8) return framesToCatFile(frames);
    return null;
  }

  public byte[] framesToIldaFile(ArrayList<Frame> frames, int ildVersion)
  {
    ArrayList<Byte> theBytes;
    theBytes = new ArrayList<Byte>();
    int frameNum = 0;

    if (frames.isEmpty() ) return null;

    for (Frame frame : frames)
    {
      //println(frame.points.size());
      theBytes.add(PApplet.parseByte('I'));
      theBytes.add(PApplet.parseByte('L'));
      theBytes.add(PApplet.parseByte('D'));
      theBytes.add(PApplet.parseByte('A'));
      theBytes.add(PApplet.parseByte(0));
      theBytes.add(PApplet.parseByte(0));
      theBytes.add(PApplet.parseByte(0));

      if (ildVersion == 0 || ildVersion == 1 || ildVersion == 2 || ildVersion == 4 || ildVersion == 5 ) theBytes.add(PApplet.parseByte(ildVersion));
      else
      {
        status.clear();
        status.add("Error: invalid ilda version");
        return null;
      }

      for (int i = 0; i < 8; i++)    //Bytes 9-16: Name
      {
        char letter;
        if (frame.frameName.length() < i+1) letter = ' ';
        else letter = frame.frameName.charAt(i);
        theBytes.add(PApplet.parseByte(letter));
      }

      if (frame.companyName.length() == 0)   //Bytes 17-24: Company Name
      {
        theBytes.add(PApplet.parseByte('I'));     //If empty: call it "IldaView"
        theBytes.add(PApplet.parseByte('l'));
        theBytes.add(PApplet.parseByte('d'));
        theBytes.add(PApplet.parseByte('a'));
        theBytes.add(PApplet.parseByte('V'));
        theBytes.add(PApplet.parseByte('i'));
        theBytes.add(PApplet.parseByte('e'));
        theBytes.add(PApplet.parseByte('w'));
      } else
      {
        for (int i = 0; i < 8; i++)  
        {
          char letter;
          if (frame.companyName.length() < i+1) letter = ' ';
          else letter = frame.companyName.charAt(i);
          theBytes.add(PApplet.parseByte(letter));
        }
      }

      //Bytes 25-26: Total point count
      theBytes.add(PApplet.parseByte((frame.points.size()>>8)&0xff));    //This better be correct
      theBytes.add(PApplet.parseByte(frame.points.size()&0xff));


      //Bytes 27-28: Frame number (automatically increment each frame)
      theBytes.add(PApplet.parseByte((++frameNum>>8)&0xff));    //This better be correct
      theBytes.add(PApplet.parseByte(frameNum&0xff));


      //Bytes 29-30: Number of frames
      theBytes.add(PApplet.parseByte((frames.size()>>8)&0xff));    //This better be correct
      theBytes.add(PApplet.parseByte(frames.size()&0xff));

      theBytes.add(PApplet.parseByte(frame.scannerHead));    //Byte 31 is scanner head
      theBytes.add(PApplet.parseByte(0));                    //Byte 32 is future

      // Ilda V0: 3D, palette
      if (ildVersion == 0)
      {
        for (Point point : frame.points)
        {
          int posx = PApplet.parseInt(constrain(map(point.position.x, 0, width, -32768, 32767), -32768, 32768));
          theBytes.add(PApplet.parseByte((posx>>8)&0xff));   
          theBytes.add(PApplet.parseByte(posx & 0xff));

          int posy = PApplet.parseInt(constrain(map(point.position.y, height, 0, -32768, 32767), -32768, 32768));
          theBytes.add(PApplet.parseByte((posy>>8)&0xff));   
          theBytes.add(PApplet.parseByte(posy & 0xff));

          int posz = PApplet.parseInt(constrain(map(point.position.z, 0, depth, -32768, 32767), -32768, 32768));    
          theBytes.add(PApplet.parseByte((posz>>8)&0xff));   
          theBytes.add(PApplet.parseByte(posz & 0xff));

          if (point.blanked)
          {
            theBytes.add(PApplet.parseByte(unbinary("01000000")));
          } else
          {
            theBytes.add(PApplet.parseByte(0));
          }
          theBytes.add(PApplet.parseByte(point.paletteIndex));
        }
      }

      //Ilda V1: 2D, palettes
      if (ildVersion == 1)
      {
        for (Point point : frame.points)
        {
          int posx = PApplet.parseInt(constrain(map(point.position.x, 0, width, -32768, 32767), -32768, 32768));
          theBytes.add(PApplet.parseByte((posx>>8)&0xff));   
          theBytes.add(PApplet.parseByte(posx & 0xff));

          int posy = PApplet.parseInt(constrain(map(point.position.y, height, 0, -32768, 32767), -32768, 32768));
          theBytes.add(PApplet.parseByte((posy>>8)&0xff));   
          theBytes.add(PApplet.parseByte(posy & 0xff));

          if (point.blanked)
          {
            theBytes.add(PApplet.parseByte(unbinary("01000000")));
          } else
          {
            theBytes.add(PApplet.parseByte(0));
          }
          theBytes.add(PApplet.parseByte(point.paletteIndex));
        }
      }

      //Huh? This isn't supposed to be here. Oh well, better be prepared.
      if (ildVersion == 2)
      {
        byte[] sfq = getActivePalette().paletteToBytes();
        for (int i = 0; i < sfq.length; i++)
        {
          theBytes.add(sfq[i]);
        }
      }

      //Ilda V4: 3D, BGR (why not RGB? Because reasons)
      if (ildVersion == 4)
      {
        for (Point point : frame.points)
        {
          int posx = PApplet.parseInt(constrain(map(point.position.x, 0, width, -32768, 32767), -32768, 32768));
          theBytes.add(PApplet.parseByte((posx>>8)&0xff));   
          theBytes.add(PApplet.parseByte(posx & 0xff));

          int posy = PApplet.parseInt(constrain(map(point.position.y, height, 0, -32768, 32767), -32768, 32768));
          theBytes.add(PApplet.parseByte((posy>>8)&0xff));   
          theBytes.add(PApplet.parseByte(posy & 0xff));

          int posz = PApplet.parseInt(constrain(map(point.position.z, 0, depth, -32768, 32767), -32768, 32768));    
          theBytes.add(PApplet.parseByte((posz>>8)&0xff));   
          theBytes.add(PApplet.parseByte(posz & 0xff));

          if (point.blanked) theBytes.add(PApplet.parseByte(unbinary("01000000")));
          else theBytes.add(PApplet.parseByte(0));

          int c = point.colour;
          if (point.blanked) c = color(0, 0, 0);  //some programs only use colour information to determine blanking

          int red = (c >> 16) & 0xFF;  // Faster way of getting red(argb)
          int green = PApplet.parseInt((c >> 8) & 0xFF);   // Faster way of getting green(argb)
          int blue = PApplet.parseInt(c & 0xFF);          // Faster way of getting blue(argb)

          theBytes.add(PApplet.parseByte(blue));
          theBytes.add(PApplet.parseByte(green));
          theBytes.add(PApplet.parseByte(red));
        }
      }

      //Ilda V5: 2D, BGR
      if (ildVersion == 5)
      {
        for (Point point : frame.points)
        {
          int posx = PApplet.parseInt(constrain(map(point.position.x, 0, width, -32768, 32767), -32768, 32768));  
          theBytes.add(PApplet.parseByte((posx>>8)&0xff));   
          theBytes.add(PApplet.parseByte(posx & 0xff));

          int posy = PApplet.parseInt(constrain(map(point.position.y, height, 0, -32768, 32767), -32768, 32768));
          theBytes.add(PApplet.parseByte((posy>>8)&0xff));   
          theBytes.add(PApplet.parseByte(posy & 0xff));



          if (point.blanked) theBytes.add(PApplet.parseByte(unbinary("01000000")));
          else theBytes.add(PApplet.parseByte(0));

          int c = point.colour;
          if (point.blanked) c = color(0, 0, 0);    //some programs only use colour information to determine blanking

          int red = (c >> 16) & 0xFF;  // Faster way of getting red(argb)
          int green = PApplet.parseInt((c >> 8) & 0xFF);   // Faster way of getting green(argb)
          int blue = PApplet.parseInt(c & 0xFF);          // Faster way of getting blue(argb)

          theBytes.add(PApplet.parseByte(blue));
          theBytes.add(PApplet.parseByte(green));
          theBytes.add(PApplet.parseByte(red));
        }
      }
    }

    //File should always end with a header

    theBytes.add(PApplet.parseByte('I'));
    theBytes.add(PApplet.parseByte('L'));
    theBytes.add(PApplet.parseByte('D'));
    theBytes.add(PApplet.parseByte('A'));
    theBytes.add(PApplet.parseByte(0));
    theBytes.add(PApplet.parseByte(0));
    theBytes.add(PApplet.parseByte(0));
    theBytes.add(PApplet.parseByte(ildVersion));

    theBytes.add(PApplet.parseByte('L'));
    theBytes.add(PApplet.parseByte('A'));
    theBytes.add(PApplet.parseByte('S'));
    theBytes.add(PApplet.parseByte('T'));
    theBytes.add(PApplet.parseByte(' '));
    theBytes.add(PApplet.parseByte('O'));
    theBytes.add(PApplet.parseByte('N'));
    theBytes.add(PApplet.parseByte('E'));

    theBytes.add(PApplet.parseByte('I'));     
    theBytes.add(PApplet.parseByte('l'));
    theBytes.add(PApplet.parseByte('d'));
    theBytes.add(PApplet.parseByte('a'));
    theBytes.add(PApplet.parseByte('V'));
    theBytes.add(PApplet.parseByte('i'));
    theBytes.add(PApplet.parseByte('e'));
    theBytes.add(PApplet.parseByte('w'));

    theBytes.add(PApplet.parseByte(0));
    theBytes.add(PApplet.parseByte(0));

    theBytes.add(PApplet.parseByte(0));
    theBytes.add(PApplet.parseByte(0));

    theBytes.add(PApplet.parseByte(0));
    theBytes.add(PApplet.parseByte(0));

    theBytes.add(PApplet.parseByte(0));

    theBytes.add(PApplet.parseByte(0));




    byte [] bt = new byte[theBytes.size()]; //Ugh!
    for (int i = 0; i<theBytes.size (); i++)
    {
      bt[i] = theBytes.get(i);
    }


    return bt;
  }

  public ArrayList<Frame> getFramesFromBytes()
  {
    ArrayList<Frame> theFrames = new ArrayList<Frame>();
    if (b == null) return null;

    if (b.length < 32)
    {
      //There isn't even a complete header here!
      status.add("Invalid file");
      println("invalid file");
      return null;
    }

    //Check if the first four bytes read ILDA:

    char[] theHeader = new char[4];
    for (int i = 0; i < 4; i++) {
      theHeader[i] = PApplet.parseChar(abs(b[i]));
    }
    String hdr = new String(theHeader);
    if ( !hdr.equals("ILDA") )
    {
      status.add("Error: file not an Ilda file. Loading cancelled.");
      status.add("Expected \"ILDA\", found \"" + hdr + "\"");
      b = new byte[0];
      return null;
    }

    //Retrieve where "ILDA" is found inside the file:

    framePositions = getFramePositions();
    //This actually returns the number of headers, and is normally one more than the real number of frames


      //This should never be true, because there was already a check if the file starts with an Ilda string
    if (framePositions == null)
    {
      status.add("No frames found.");
      return null;
    }

    Frame frame = new Frame();

    //If there is only one header, read until the end
    if (framePositions.size() == 1)
    {
      frame = readFrame(0, b.length-1);
      if (frame != null) theFrames.add(frame);
    } else
    {
      //In case of multiple headers, read from current to next header
      //This is actually a bad way to do it because "ILDA" can occur by accident inside the data
      //Though as this chance is rare I decided to take the risk
      //And by "rare" I mean one in 72 quadrillion (1/256^7) for each byte
      //If you ever encounter such a frame, I'll send you a cookie
      for (int i = 1; i<framePositions.size (); i++) 
      {
        //Skip to next ILDA occurence when ILDA occurs in the header
        if (framePositions.get(i) - framePositions.get(i-1) <= 32 && (i+1) < framePositions.size())
        {
          frame = readFrame(framePositions.get(i-1), framePositions.get(i+1));
          if (frame != null) theFrames.add(frame);
        } else
        {
          //Read the frame between this and the next header
          frame = readFrame(framePositions.get(i-1), framePositions.get(i));
          if (frame != null) theFrames.add(frame);
        }
      }
    }
    return theFrames;
  }

  //Read the file and convert them to Frames, which get added to the frames ArrayList
  public void readFile()
  {
    ArrayList<Frame> newFrames = getFramesFromBytes();
    for (Frame frame : newFrames)
    {
      frames.add(frame);
    }

    status.add("Loaded file. " + framePositions.size() + " ILDA header(s) detected.");
  }

  //Where can "ILDA" be found in the file?

  public IntList getFramePositions()
  {
    IntList positions = new IntList();
    for (int j=0; j<b.length-6; j++)
    {
      if (PApplet.parseChar(b[j]) == 'I' && PApplet.parseChar(b[j+1]) == 'L' && PApplet.parseChar(b[j+2]) == 'D' && PApplet.parseChar(b[j+3]) == 'A' && b[j+4] == 0 && b[j+5] == 0 && b[j+5] == 0)
      {
        positions.append(j);
      }
    }
    return positions;
  }

  //Read the file between the indices offset and end

  public Frame readFrame(int offset, int end)
  {

    if (offset > end-32)
    {
      println("Invalid frame");
      return null;
    }
    if (offset >= b.length - 32)
    {
      return null;
    }

    if (end >= b.length)
    {
      return null;
    }

    //Check if it does read ILDA (if it doesn't, check getFramePositions() ):
    char[] theHeader = new char[4];
    for (int i = 0; i < 4; i++) {
      theHeader[i] = PApplet.parseChar(abs(b[i]));
    }
    String hdr = new String(theHeader);
    if ( !hdr.equals("ILDA") )
    {
      status.add("Error: file not an Ilda file. Loading cancelled.");
      status.add("Expected \"ILDA\", found \"" + hdr + "\"");
      return null;
    }

    //Read header data:

    //Bytes 8-15: frame name
    char[] name = new char[8];
    for (int i = 0; i < 8; i++) {
      name[i] = PApplet.parseChar(abs(b[i+8+offset]));
    } 

    //Bytes 16-23: company name
    char[] company = new char[8];
    for (int i = 0; i < 8; i++) {
      company[i] = PApplet.parseChar(abs(b[i+16+offset]));
    } 

    //Bytes 24-25: point count in frames or total colours in palettes
    byte[] pointCountt = new byte[2];
    pointCountt[0] = b[24+offset];
    pointCountt[1] = b[25+offset];

    //Bytes 26-27: frame number in frames or palette number in palettes
    byte[] frameNumber = new byte[2];
    frameNumber[0] = b[26+offset];
    frameNumber[1] = b[27+offset];

    //Unsupported format detection:
    if (PApplet.parseInt(b[7+offset]) != 0 && PApplet.parseInt(b[7+offset]) != 1 && PApplet.parseInt(b[7+offset]) != 2 && PApplet.parseInt(b[7+offset]) != 4 && PApplet.parseInt(b[7+offset]) != 5)
    {
      status.add("Unsupported file format: " + PApplet.parseInt(b[7+offset]));
      return null;
    }

    //Is this a palette or a frame? 2 = palette, rest = frame
    if ( PApplet.parseInt(b[7+offset]) == 2)
    {
      status.add("Palette included");
      Palette palette = new Palette();

      palette.name = new String(name);
      palette.companyName = new String(company);
      palette.totalColors = unsignedShortToInt(pointCountt);

      //Byte 30: scanner head.
      palette.scannerHead = PApplet.parseInt(b[30+offset]);

      palette.formHeader();

      // ILDA V2: Palette information

      for (int i = 32+offset; i<end; i+=3)
      {
        palette.addColour(PApplet.parseInt(b[i]), PApplet.parseInt(b[i+1]), PApplet.parseInt(b[i+2]));
      }


      palettes.add(palette);
      activePalette = palettes.size()-1;
      return null;
    } else
    {
      Frame frame = new Frame();  //Frame(this);      <- remains here as a symbol of how not to program

      //Byte 7 = Ilda file version
      frame.ildaVersion = PApplet.parseInt(b[7+offset]);

      //Information previously read out (because palettes also have them):
      frame.frameName = new String(name);
      frame.companyName = new String(company);
      frame.pointCount = unsignedShortToInt(pointCountt);
      frame.frameNumber = unsignedShortToInt(frameNumber);

      if (frame.frameName.equals("EastrEgg") && frame.companyName.equals("IldaView")) status.add("Congratulations, you found an egg!");

      //Bytes 28-29: total number of frames (not used in palettes)
      byte[] numberOfFrames = new byte[2];
      numberOfFrames[0] = b[28+offset];
      numberOfFrames[1] = b[29+offset];
      frame.totalFrames = unsignedShortToInt(numberOfFrames);

      //Byte 30: scanner head. 
      frame.scannerHead = PApplet.parseInt(b[30+offset]);

      frame.formHeader();

      // Read the points:

      // ILDA V0 (3D, Palettes)
      if (b[7+offset] == 0)
      {
        frame.palette = true;
        for (int i = 32+offset; i < end; i += 8)
        {
          if (!(i >= b.length-8))
          {
            byte[] x = new byte[2];
            x[0] = b[i];
            x[1] = b[i+1];
            float X = PApplet.parseFloat(unsignedShortToInt(x));
            byte[] y = new byte[2];
            y[0] = b[i+2];
            y[1] = b[i+3];
            float Y = PApplet.parseFloat(unsignedShortToInt(y));
            byte[] z = new byte[2];
            z[0] = b[i+4];
            z[1] = b[i+5];
            float Z = PApplet.parseFloat(unsignedShortToInt(z));

            X = map(X, -32768, 32767, 0, width);
            Y = map(Y, 32768, -32767, 0, height);
            Z = map(Z, 32768, -32768, 0, depth);

            String statusString = binary(b[i+6])+binary(b[i+7]);
            boolean bl = false;
            if (statusString.charAt(1) == '1') bl = true;

            Point point = new Point(X, Y, Z, PApplet.parseInt(b[i+7]), bl);
            frame.points.add(point);
          }
        }

        frame.palettePaint(getActivePalette());
      }


      // ILDA V1: 2D, Palettes
      if (b[7+offset] == 1)
      {
        frame.palette = true;
        for (int i = 32+offset; i < end; i+=6)
        {
          if (!(i >= b.length-6))
          {
            byte[] x = new byte[2];
            x[0] = b[i];
            x[1] = b[i+1];
            float X = PApplet.parseFloat(unsignedShortToInt(x));
            byte[] y = new byte[2];
            y[0] = b[i+2];
            y[1] = b[i+3];
            float Y = PApplet.parseFloat(unsignedShortToInt(y));
            X = map(X, -32768, 32767, 0, width);
            Y = map(Y, 32768, -32767, 0, height);

            String statusString = binary(b[i+4])+binary(b[i+5]);
            boolean bl = false;
            if (statusString.charAt(1) == '1') bl = true;


            Point point = new Point(X, Y, depth/2, PApplet.parseInt(b[i+5]), bl);
            frame.points.add(point);
          }
        }

        frame.palettePaint(getActivePalette());
      }

      // ILDA V4: 3D, BGR
      if (b[7+offset] == 4)
      {
        for (int i = 32+offset; i < end; i+=10)
        {
          if (!(i >= b.length-10))
          {
            byte[] x = new byte[2];
            x[0] = b[i];
            x[1] = b[i+1];
            float X = PApplet.parseFloat(unsignedShortToInt(x));
            byte[] y = new byte[2];
            y[0] = b[i+2];
            y[1] = b[i+3];
            float Y = PApplet.parseFloat(unsignedShortToInt(y));
            byte[] z = new byte[2];
            z[0] = b[i+4];
            z[1] = b[i+5];
            float Z = PApplet.parseFloat(unsignedShortToInt(z));

            X = map(X, -32768, 32767, 0, width);
            Y = map(Y, 32768, -32767, 0, height);
            Z = map(Z, -32768, 32767, 0, depth);

            String statusString = binary(b[i+6]);
            boolean bl = false;
            if (statusString.charAt(1) == '1') bl = true;



            Point point = new Point(X, Y, Z, abs(PApplet.parseInt(b[i+9])), abs(PApplet.parseInt(b[i+8])), abs(PApplet.parseInt(b[i+7])), bl);
            frame.points.add(point);
          }
        }
      }

      //ILDA V5: 2D, BGR values
      //Why not RGB? Because reasons
      if (b[7+offset] == 5)
      {
        for (int i = 32+offset; i < end; i+=8)
        {
          if (!(i >= b.length-8))
          {
            byte[] x = new byte[2];
            x[0] = b[i];
            x[1] = b[i+1];
            float X = PApplet.parseFloat(unsignedShortToInt(x));
            byte[] y = new byte[2];
            y[0] = b[i+2];
            y[1] = b[i+3];
            float Y = PApplet.parseFloat(unsignedShortToInt(y));

            X = map(X, -32768, 32767, 0, width);
            Y = map(Y, 32768, -32767, 0, height);

            String statusString = binary(b[i+4]);
            boolean bl = false;
            if (statusString.charAt(1) == '1') bl = true;

            Point point = new Point(X, Y, depth/2, abs(PApplet.parseInt(b[i+7])), abs(PApplet.parseInt(b[i+6])), abs(PApplet.parseInt(b[i+5])), bl);
            frame.points.add(point);
          }
        }
      }

      return frame;
    }
  }

  public Frame convertPicToIldaFrame()
  {
    if (b.length > 0)
    {
      int begin = b[0] == 0 ? 15 : 14;
      return convertPicToIldaFrame(b, begin, b.length, b[0]);
    }
    return null;
  }

  public Frame convertPicToIldaFrame(byte[] b, int offset, int end, int version)
  {
    //Version number (unofficial):
    //1 = palette only, no RGB coordinates: XXYYZZPalStat
    //4 = RGB coordinates in addition to a palette number: XXYYZZPalStatRGB (ini file has a setting to use palette index but by default rgb is used for colour)

    int bbp = (version == 1 || version == 0) ? 8 : 11;  //bits per point

    if (b.length < offset) return null;
    Frame frame = new Frame();

    for (int i = offset; i < end; i+=bbp)
    {
      if ((i <= end-bbp))
      {
        byte[] x = new byte[2];
        x[1] = b[i];
        x[0] = b[i+1];
        float X = PApplet.parseFloat(unsignedShortToInt(x));
        byte[] y = new byte[2];
        y[1] = b[i+2];
        y[0] = b[i+3];
        float Y = PApplet.parseFloat(unsignedShortToInt(y));
        byte[] z = new byte[2];
        z[1] = b[i+4];
        z[0] = b[i+5];
        float Z = PApplet.parseFloat(unsignedShortToInt(z));

        X = map(X, -32768, 32767, 0, width);
        Y = map(Y, -32768, 32767, 0, height);
        Z = map(Z, -32768, 32767, 0, depth);

        boolean bl = false;
        boolean normalvector = false; //ignore normalvectors

        int palIndex = 0;

        if ((b[i+6] & 0x40) == 64) bl = true;
        if ((b[i+6] & 0x80) != 128) normalvector = true;
        palIndex = b[i+6] & 0x3F;

        if (!normalvector)
        {
          Point point;
          if (version == 1)
          {
            point = new Point(X, Y, Z, palIndex, bl);
          } else
          {
            point = new Point(X, Y, Z, PApplet.parseInt(b[i+8]), PApplet.parseInt(b[i+9]), PApplet.parseInt(b[i+10]), bl);
            point.paletteIndex = palIndex;
          }
          frame.points.add(point);
        }
      }
    }
    if (picUsesPalette || version == 1)
    {
      frame.palette = true;
      frame.palettePaint(getActivePalette());
    }
    frame.pointCount = frame.points.size();
    frame.formHeader();
    return frame;
  }

  public ArrayList<Frame> convertCatToFrames()
  {
    return convertCatToFrames(b);
  }

  public ArrayList<Frame> convertCatToFrames(byte[] b)
  {
    if (b.length < 3) 
    {
      status.add("Invalid file");
      return null;
    }

    ArrayList<Frame> fl = new ArrayList<Frame>();

    int count = 1;
    for (int i = 0; i < b.length; )
    {
      int version = b[i];

      byte[] length = {
        b[2+i], b[1+i]
      };
      int l = unsignedShortToInt(length);
      int size = abs(l*((version == 0) ? 8 : 11));

      Frame f = convertPicToIldaFrame(b, i+3, i+3+size, version==0?1:4);
      f.frameNumber = count++;

      if (!(removeEmptyCatFrames && f.points.isEmpty())) fl.add(f);

      i += 3+size;
    }

    return fl;
  }

  public byte[] frameToPicFile(Frame frame)
  {
    byte[] b = new byte[14];
    b[0] = 4;
    b[6] = (byte) (((short) frame.points.size() >> 8) & 0xff);
    b[7] = (byte) (frame.points.size() & 0xff);

    int i = 0;
    for (Point point : frame.points)
    {
      byte[] pb = createPicPoint(point, true);
      b = concat(b, pb);
    }
    return b;
  }

  public byte[] createPicPoint(Point point, boolean rgb)
  {
    byte[] b;
    if (rgb) b = new byte[11];
    else b = new byte[8];

    short x = (short) map(point.position.x, 0, width, -32768, 32767);
    short y = (short) map(point.position.y, 0, height, -32768, 32767);
    short z = (short) map(point.position.z, 0, depth, -32768, 32767);

    b[1] = (byte) (((x) >> 8) & 0xff); //X
    b[0] = (byte) ((x) & 0xff);
    b[3] = (byte) (((y) >> 8) & 0xff); //Y
    b[2] = (byte) ((y) & 0xff);
    b[5] = (byte) (((z) >> 8) & 0xff); //Z
    b[4] = (byte) ((z) & 0xff);
    b[6] = (byte) ((((short) point.paletteIndex) >> 6) & 0x3f | (point.blanked ? 0x40 : 0x0) | 0x80); //pal | blanked | normalvector
    b[7] = 0; //we don't do parts and repeat here
    if (rgb)
    {
      b[8] = (byte) ((point.colour >> 16) & 0xff);
      b[9] = (byte) ((point.colour >> 8) & 0xff);
      b[10] = (byte) (point.colour & 0xff);
    }

    return b;
  }

  public byte[] framesToOldCatFile(ArrayList<Frame> frames)
  {
    ArrayList<byte[]> theB = new ArrayList<byte[]>();
    for (int i = 0; i < 32477; i++)
    {
      byte[] b = {
        0, 0, 0
      };
      theB.add(b);
    }
    int i = 0;
    for (Frame frame : frames)
    {
      byte[] b = new byte[3];
      b[0] = 0;
      b[1] = (byte) (((short) frame.points.size() >> 8) & 0xff);
      b[2] = (byte) ((short) frame.points.size()  & 0xff);
      for (Point point : frame.points)
      {
        b = concat(b, createPicPoint(point, false));
      }
      if (respectFrameNumberWhenExportingToCat) theB.set(frame.frameNumber, b);
      else theB.set(i++, b);
    }

    byte[] out = new byte[0];

    for (byte[] b : theB)
    {
      out = concat(out, b);
    }

    return out;
  }

  public byte[] framesToCatFile(ArrayList<Frame> frames)
  {
    ArrayList<byte[]> theB = new ArrayList<byte[]>();

    for (Frame frame : frames)
    {
      byte[] b = new byte[3];
      b[0] = 1;
      b[2] = (byte) (((short) frame.points.size() >> 8) & 0xff);
      b[1] = (byte) ((short) frame.points.size()  & 0xff);
      for (Point point : frame.points)
      {
        b = concat(b, createPicPoint(point, true));
      }
      if (respectFrameNumberWhenExportingToCat) 
      {
        if (frame.frameNumber < theB.size()) theB.set(frame.frameNumber, b);
        else
        {
          for (int i = theB.size (); i < frame.frameNumber; i++)
          {
            byte[] bt = {
              1, 0, 0
            };
            theB.add(bt);
          }
          theB.add(b);
        }
      } else theB.add(b);
    }

    byte[] out = new byte[0];

    for (byte[] b : theB)
    {
      out = concat(out, b);
    }

    return out;
  }




  //Thanks heroic!

  public final int unsignedShortToInt(byte[] b)
  {

    /*
  [02:02]  heroic  | is bitwise or
     [02:02]  heroic  << is bitwise shift left
     [02:06]  heroic  cmb: the & 0xff in the code I pasted (the method version) is defeating sign extension and causing your negative values to disappear
     */
    if ( b.length != 2)
    {
      throw new IllegalArgumentException();
    }
    int i = 0;
    i |= b[0] ;
    i <<= 8;
    i |= b[1] & 0xff;
    return i;
  }
}


class Frame
{

  /*
   * A Frame stores all its Points and has some methods to display them
   * and to save header data
   */

  ArrayList<Point> points = new ArrayList<Point>();
  //The Points in the Frame


  int ildaVersion;    //Data retrieved from header
  String frameName;
  String companyName;
  int pointCount;
  int frameNumber;
  int totalFrames;
  int scannerHead;
  StringList hoofding = new StringList(); //Data now in a nice StringList to display 
  boolean palette = false;

  Frame()
  {
  }

  Frame(Frame frame)
  {
    this.points = new ArrayList<Point>(frame.points.size());
    for (Point point : frame.points)
    {
      this.points.add(point.clone(point));
    }
    this.ildaVersion = frame.ildaVersion;
    if (frame.frameName != null) this.frameName = new String(frame.frameName);
    else frameName = "";
    if (frame.companyName != null) this.companyName = new String(frame.companyName);
    else companyName = "IldaViewer";
    this.pointCount = frame.pointCount;
    this.frameNumber = frame.frameNumber;
    this.totalFrames = frame.totalFrames;
    this.scannerHead = frame.scannerHead;
    this.palette = frame.palette;
  }
  
  public Frame writeProperties(Frame frame1, Frame frame2)
  {
    frame2.ildaVersion = frame1.ildaVersion;
    frame2.frameName = frame1.frameName;
    frame2.companyName = frame1.companyName;
    frame2.pointCount = frame1.pointCount;
    frame2.frameNumber = frame1.frameNumber;
    frame2.totalFrames = frame1.totalFrames;
    frame2.scannerHead = frame1.scannerHead;
    frame2.palette = frame1.palette;
    
    return frame2;
  }

  public Frame clone()
  {
    Frame frame = new Frame(this);
    return frame;
  }


  //This method changes the colour of each point according to the active palette
  public void palettePaint(Palette palette)
  {
    for ( Point point : points)
    {
      point.setColourFromPalette(palette);
    }
  }

  //This puts all the header information inside the hoofding StringList

  public void formHeader()
  {
    hoofding.clear();
    hoofding.append("Ilda version " + ildaVersion);
    hoofding.append("Frame: " + frameName);
    hoofding.append("Company: " + companyName);
    hoofding.append("Point count: " + pointCount);
    hoofding.append("Frame number: " + frameNumber);
    hoofding.append("Total frames: " + totalFrames);
    hoofding.append("Scanner head: " + scannerHead);
  }

  //Returns the real point count

  public int getPointCount()
  {
    return points.size();
  }

  //By default, draw the frame with visible blanking points
  public void drawFrame()
  {
    drawFrame(true);
  }

  //Displays all the points inside this frame with a line in between them:



  public void drawFrame(boolean showBlankedPoints)
  {

    if (!loading)
    {
      boolean firstPoint = true;
      float oldpositionx = 0; 
      float oldpositiony = 0;
      float oldpositionz = 0;
      for (Point point : points)
      {
        if (showBlankedPoints || !point.blanked) point.displayPoint();


        PVector position = point.getPosition();

        if (!firstPoint)
        {
          strokeWeight(1);  
          if (!showBlankedPoints && point.blanked) stroke(0);   
          else
          {   
            line(position.x, position.y, position.z, oldpositionx, oldpositiony, oldpositionz);
          }
          oldpositionx = position.x;
          oldpositiony = position.y;
          oldpositionz = position.z;
        } else
        {
          firstPoint = false;
          oldpositionx = position.x;
          oldpositiony = position.y;
          oldpositionz = position.z;
        }
      }
    } else println("Warning: tried to display while loading file");
  }

  public void drawFrame(float offX, float offY, float offZ, float sizeX, float sizeY, float sizeZ)
  {
    drawFrame(g, offX, offY, offZ, sizeX, sizeY, sizeZ);
  }

  public void drawFrame(PGraphics pg, float offX, float offY, float offZ, float sizeX, float sizeY, float sizeZ)
  {
    drawFrame(pg, offX, offY, offZ, sizeX, sizeY, sizeZ, true, true);
  }


  public void drawFrame(float offX, float offY, float offZ, float sizeX, float sizeY, float sizeZ, boolean showBlankedPoints, boolean clipping)
  {
    drawFrame(g, offX, offY, offZ, sizeX, sizeY, sizeZ, showBlankedPoints, clipping);
  }

  public void drawFrame(PGraphics pg, float offX, float offY, float offZ, float sizeX, float sizeY, float sizeZ, boolean showBlankedPoints, boolean clipping)
  {
    //pg.beginDraw();
    boolean firstPoint = true;
    float oldpositionx = 0; 
    float oldpositiony = 0;
    float oldpositionz = 0;
    for (Point point : points)
    {
      PVector position = point.getPosition();

      float x = map(position.x, 0, width, 0, sizeX) + offX;
      float y = map(position.y, 0, height, 0, sizeY) + offY;
      float z = map(position.z, 0, depth, 0, sizeZ) + offZ;

      if (clipping)
      {
        if (x < offX) x = offX;
        if (y < offY) y = offY;
        if (z < offZ) z = offZ;
        if (x > offX + sizeX) x = offX + sizeX;
        if (y > offY + sizeY) y = offY + sizeY;
        if (z > offZ + sizeZ) z = offZ + sizeZ;
      }

      if (showBlankedPoints || !point.blanked)
      {
        pg.strokeWeight(3);
        pg.stroke(point.colour);
        if (point.blanked) pg.stroke(75, 75, 75);
        pg.point(x, y, z);
      }



      if (!firstPoint)
      {
        pg.strokeWeight(1);  
        if (!showBlankedPoints && point.blanked) pg.stroke(0);   
        else
        {   
          pg.line(x, y, z, oldpositionx, oldpositiony, oldpositionz);
        }
        oldpositionx = x;
        oldpositiony = y;
        oldpositionz = z;
      } else
      {
        firstPoint = false;
        oldpositionx = x;
        oldpositiony = y;
        oldpositionz = z;
      }
    }
    //pg.endDraw();
  }

  //Translates all points with a PVector
  public void translate(PVector newposition)
  {
    for (Point point : points)
    {
      point.position.add(newposition);
    }
  }

  public void fitColourIndexWithPalette(Palette palette)
  {
    for (Point point : points)
    {
      point.paletteIndex = point.getBestFittingPaletteColourIndex(palette);
    }
  }

  public void merge(Frame frame)
  {
    if (frame.points != null && frame.points.size() > 0) 
    {
      Point p = frame.points.get(frame.points.size()-1);
      p.blanked = true;
      points.add(p);
    }
    points.addAll(frame.points);
  }

  public void merge(Frame frame, float[] mergedInformation)
  {
    if (mergedInformation.length != 10) return;

    // 0: frame header
    if (mergedInformation[0] != 0)
    {
      ildaVersion = frame.ildaVersion;
      frameName = frame.frameName;
      companyName = frame.companyName;
      frameNumber = frame.frameNumber;
      scannerHead = frame.scannerHead;
    }

    // 1: Point number
    if (mergedInformation[1] != 0)
    {
      if (points.size() < frame.points.size())
      {
        for (int i = points.size (); i < frame.points.size(); i++)
        {
          points.add(frame.points.get(i));
        }
      } else
      {
        for (int i = points.size ()-1; i >= frame.points.size(); i--)
        {
          points.remove(i);
        }
      }
    }

    // 2: X
    if (mergedInformation[2] != 0)
    {
      for (int i = 0; i < min (points.size (), frame.points.size()); i++)
      {
        points.get(i).position.x = frame.points.get(i).position.x;
      }
    }

    // 3: R
    if (mergedInformation[3] != 0)
    {
      for (int i = 0; i < min (points.size (), frame.points.size()); i++)
      {
        points.get(i).colour = color((frame.points.get(i).colour >> 16) & 0xFF, (points.get(i).colour >> 8) & 0xFF, points.get(i).colour & 0xFF);
      }
    }

    // 4: Y
    if (mergedInformation[4] != 0)
    {
      for (int i = 0; i < min (points.size (), frame.points.size()); i++)
      {
        points.get(i).position.y = frame.points.get(i).position.y;
      }
    }

    // 5: G
    if (mergedInformation[5] != 0)
    {
      for (int i = 0; i < min (points.size (), frame.points.size()); i++)
      {
        points.get(i).colour = color((points.get(i).colour >> 16) & 0xFF, (frame.points.get(i).colour >> 8) & 0xFF, points.get(i).colour & 0xFF);
      }
    }

    // 6: Z
    if (mergedInformation[6] != 0)
    {
      for (int i = 0; i < min (points.size (), frame.points.size()); i++)
      {
        points.get(i).position.z = frame.points.get(i).position.z;
      }
    }

    // 7: B
    if (mergedInformation[7] != 0)
    {
      for (int i = 0; i < min (points.size (), frame.points.size()); i++)
      {
        points.get(i).colour = color((points.get(i).colour >> 16) & 0xFF, (points.get(i).colour >> 8) & 0xFF, frame.points.get(i).colour & 0xFF);
      }
    }

    // 8: blanking
    if (mergedInformation[8] != 0)
    {
      for (int i = 0; i < min (points.size (), frame.points.size()); i++)
      {
        points.get(i).blanked = frame.points.get(i).blanked;
      }
    }

    // 9: palette index
    if (mergedInformation[9] != 0)
    {
      for (int i = 0; i < min (points.size (), frame.points.size()); i++)
      {
        points.get(i).paletteIndex = frame.points.get(i).paletteIndex;
      }
    }

    pointCount = points.size();
    formHeader();
  }

  public String toString()
  {
    return "Name: " + frameName + "  | Company: " + companyName + "  | Points: " + pointCount;
  }
}

public float[][] calculateRotationMatrix(float theta, float phi, float psi)
{
  float[][] R = new float[3][3]; //Rotation matrix

  //First row:
  R[0][0] = cos(phi)*cos(psi);
  R[0][1] = cos(phi)*sin(psi);
  R[0][2] = -sin(phi);

  //Second row:
  R[1][0] = sin(theta)*sin(phi)*cos(psi) - cos(theta)*sin(psi);
  R[1][1] = sin(theta)*sin(phi)*sin(psi) + cos(theta)*cos(psi);
  R[1][2] = sin(theta)*cos(phi);

  //Third row:
  R[2][0] = cos(theta)*sin(phi)*cos(psi) + sin(theta)*sin(psi);
  R[2][1] = cos(theta)*sin(phi)*sin(psi) - sin(theta)*cos(psi);
  R[2][2] = cos(theta)*cos(phi);

  return R;
}

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

public void beginFrameEditor()
{
  frameditor = true;
  if (frameEditor == null) frameEditor = new FrameEditor();
  setFrameEditorControls();
}

public void exitFrameEditor()
{
  frameditor = false;
}

public void setFrameEditorControls()
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
          cp5.getGroup("framePicker").setVisible(PApplet.parseBoolean((int) cV[i]));
          break;
        case 1:
          cp5.getGroup("rasterImage").setPosition(pos.x, pos.y);
          cp5.getGroup("rasterImage").setVisible(PApplet.parseBoolean((int) cV[i]));
          break;
        case 2:
          cp5.getGroup("rotationControl").setPosition(pos.x, pos.y);
          cp5.getGroup("rotationControl").setVisible(PApplet.parseBoolean((int) cV[i]));
          break;
        case 3:
          cp5.getGroup("sizeControl").setPosition(pos.x, pos.y);
          cp5.getGroup("sizeControl").setVisible(PApplet.parseBoolean((int) cV[i]));
          break;
        case 4:
          cp5.getGroup("positionControl").setPosition(pos.x, pos.y);
          cp5.getGroup("positionControl").setVisible(PApplet.parseBoolean((int) cV[i]));
          break;
        case 5:
          cp5.getGroup("perspective").setPosition(pos.x, pos.y);
          cp5.getGroup("perspective").setVisible(PApplet.parseBoolean((int) cV[i]));
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

public PVector findEmptyGuiSpot()
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

public void openFramedImage()
{
  status.clear();
  status.add("Select an image to load in as a background image");
  selectInput("Select an image file to load in as a background image", "backgroundImageSelected");
}

public void backgroundImageSelected(File selection) {
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

public void setSizeBackgroundFrameEd()
{
  if (frameEditor.backgroundImage.width < frameEditor.backgroundImage.height && frameEditor.backgroundImage.height > height) frameEditor.backgroundImage.resize(0, height);
  if (frameEditor.backgroundImage.width > frameEditor.backgroundImage.height && frameEditor.backgroundImage.width > width) frameEditor.backgroundImage.resize(width, 0);

  frameEditor.imgsx = frameEditor.backgroundImage.width;
  frameEditor.imgsy = frameEditor.backgroundImage.height;
  frameEditor.imgx = (int) (width*0.5f-frameEditor.backgroundImage.width*0.5f);
  frameEditor.imgy = (int) (height*0.5f-frameEditor.backgroundImage.height*0.5f);
}

public void hideFramedImage(boolean value)
{
  frameEditor.showBackground = !value;
}

public void positionFramedImage()
{
  cursor(MOVE);
  frameEditor.moveImage = true;
}

public void listFrameEd(boolean value)
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

public void sizeFramedImage(boolean value)
{

  cp5.getController("sizeXFramedImage").setVisible(value);
  cp5.getController("sizeYFramedImage").setVisible(value);
  cp5.getController("sizeXYFramedImage").setVisible(value);
}

public void sizeXYFramedImage(float value)
{
  if (frameEditor == null) return;

  if (value> frameEditor.maxs)
  {
    Slider sl = (Slider) cp5.getController("sizeXYFramedImage");
    sl.setRange(sl.getMin(), sl.getMax()+0.01f);
    frameEditor.maxs = sl.getMax()*0.9f;
    sl.getCaptionLabel().alignX(CENTER);
  }
  if (value < frameEditor.mins)
  {
    Slider sl = (Slider) cp5.getController("sizeXYFramedImage");
    sl.setRange(sl.getMin()-0.01f, sl.getMax());
    frameEditor.mins = sl.getMin()*0.9f;
    sl.getCaptionLabel().alignX(CENTER);
  }

  frameEditor.resizeBackground();
}

public void sizeXFramedImage(float value)
{
  if (frameEditor == null) return;

  if (value> frameEditor.maxsx)
  {
    Slider sl = (Slider) cp5.getController("sizeXFramedImage");
    sl.setRange(sl.getMin(), sl.getMax()+0.01f);
    frameEditor.maxsx = sl.getMax()*0.9f;
    sl.getCaptionLabel().alignX(CENTER);
  }
  if (value < frameEditor.minsx)
  {
    Slider sl = (Slider) cp5.getController("sizeXFramedImage");
    sl.setRange(sl.getMin()-0.01f, sl.getMax());
    frameEditor.minsx = sl.getMin()*0.9f;
    sl.getCaptionLabel().alignX(CENTER);
  }

  frameEditor.resizeBackground();
}

public void sizeYFramedImage(float value)
{
  if (frameEditor == null) return;

  if (value> frameEditor.maxsy)
  {
    Slider sl = (Slider) cp5.getController("sizeYFramedImage");
    sl.setRange(sl.getMin(), sl.getMax()+0.01f);
    frameEditor.maxsy = sl.getMax()*0.9f;
    sl.getCaptionLabel().alignX(CENTER);
  }
  if (value < frameEditor.minsy)
  {
    Slider sl = (Slider) cp5.getController("sizeYFramedImage");
    sl.setRange(sl.getMin()-0.01f, sl.getMax());
    frameEditor.minsy = sl.getMin()*0.9f;
    sl.getCaptionLabel().alignX(CENTER);
  }

  frameEditor.resizeBackground();
}

public void recolourFramedImage()
{
  if (frameEditor == null) return;
  frameEditor.recolourFromImage();
}

public void previousFrameEd() //manually skip to previous frame
{
  previousFrame();
  frameEditor.frame = new EditingFrame(frames.get(activeFrame));
}

public void nextFrameEd() //manually skip to next frame
{
  nextFrame();
  frameEditor.frame = new EditingFrame(frames.get(activeFrame));
}

public void firstFrameEd()
{
  firstFrame();
  frameEditor.frame = new EditingFrame(frames.get(activeFrame));
}

public void lastFrameEd()
{
  lastFrame();
  frameEditor.frame = new EditingFrame(frames.get(activeFrame));
}

public void rotationX(float value)
{
  if (frameEditor != null) frameEditor.rotX = value;
}

public void rotationY(float value)
{
  if (frameEditor != null) frameEditor.rotY = value;
}

public void rotationZ(float value)
{
  if (frameEditor != null) frameEditor.rotZ = value;
}

public void zoom(float value)
{
  if (frameEditor != null) 
  {
    frameEditor.zoomFactor = value;
    frameEditor.zooming = true;
  }
}

public void resetZoom()
{
  if (frameEditor != null) frameEditor.scale = 1;
  cp5.getController("zoom").setValue(0);
}

public void viewPerspective(float value)
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
  float maxsx = 1.25f;
  float maxsy = 1.25f;
  float minsx = -1.25f;
  float minsy = -1.25f;
  float maxs = 1.25f;
  float mins = -1.25f;
  boolean listFrames = false;
  int frameSize = 100;
  int xoffset = 5;
  int visibleFrames = PApplet.parseInt(width/frameSize);
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

  public void update()
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


  public void resizeBackground()
  {
    if (sourceBackgroundImage == null) return;

    int newSizeX = (int) (imgsx*cp5.getController("sizeXYFramedImage").getValue()*cp5.getController("sizeXFramedImage").getValue());
    int newSizeY = (int) (imgsy*cp5.getController("sizeXYFramedImage").getValue()*cp5.getController("sizeYFramedImage").getValue());

    imgx += (backgroundImage.width-abs(newSizeX))*0.5f;
    imgy += (backgroundImage.height-abs(newSizeY))*0.5f;

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

  public void recolourFromImage()
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

  public void listFrames()
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
        frames.get(i).drawFrame((float)xoffset+frameSize*i, pickerY+10, 0.0f, 90.0f, 90.0f, 0.0f, false, true);
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

  public void display()
  {
    display(0, 0, width, height, 0, 0, 0, 1);
  }

  public void display(int x, int y, float sizex, float sizey, float rotx, float roty, float rotz, float projection)
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
      float xind = point.position.x-width*0.5f;
      float yind = point.position.y-height*0.5f;
      float zind = point.position.z-depth*0.5f;

      float xnew = sizex*(R[0][0]*xind + R[0][1]*yind + R[0][2]*zind);
      float ynew = sizey*(R[1][0]*xind + R[1][1]*yind + R[1][2]*zind);
      if (projection != 0)    //perspective
      {
        float znew = R[2][0]*xind + R[2][1]*yind + R[2][2]*zind;  
        znew = znew / depth;
        xnew *= znew/(projection);
        ynew *= znew/(projection);
      }  

      xnew += width*0.5f;
      ynew += height*0.5f;

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

  public void displayPoint(int x, int y, int z)
  {
    setPointColour();
    point(x, y, z);
  }

  public void displayPoint(float x, float y, float z)
  {
    setPointColour();
    point(x, y, z);
  }

  public void displayPoint()
  {
    displayPoint(position.x, position.y, position.z);
  }

  public void setPointColour()
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
      red = PApplet.parseInt(sin(PApplet.parseFloat(frameCount)/5)*127+128);
      strokeWeight(6);
    }
    stroke(red, green, blue);
  }
}

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

  public void display(float elx, float ely)
  {
    display(g, elx, ely);
  }

  public void display(PGraphics pgr, float elx, float ely)
  {
  }

  public void update(float elx, float ely)
  {
  }

  public boolean activateElement(float elx, float ely)
  {
    if (mouseX > x + elx && mouseX < x + elx + sizex && mouseY > y + ely && mouseY < y + ely + sizey)
    {
      active = true;
      return true;
    }
    return false;
  }

  public void toggle()
  {
  }

  public float getValue()
  {
    return 0;
  }

  public void setValue(float input)
  {
  }

  public void activate(boolean activate)
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

  public void display(PGraphics pgr, float elx, float ely)
  {
    if (pgr != g) pgr.beginDraw();
    pgr.fill(50, alpha);
    if (active)
    {
      if (laserboyMode) pgr.fill(color(PApplet.parseInt((sin((ely)/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((y)+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((elx+ely)/2+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255)), alpha); 
      else pgr.fill(127, alpha);
      pgr.stroke(0);
      pgr.strokeWeight(1);
    } else
    {
      if (laserboyMode) pgr.fill(color(PApplet.parseInt((sin((elx)/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), 255-PApplet.parseInt((sin((y)+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((x+y)/2+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255)), alpha); 
      else pgr.fill(50, alpha);
      noStroke();
    }
    pgr.rect(x+elx, y+ely, sizex, sizey);
    if (laserboyMode) pgr.fill(color(255-PApplet.parseInt((sin((x)/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt(255-(sin((y)+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), 255-PApplet.parseInt((sin((elx+ely)/2+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255)), alpha); 
    else pgr.fill(255, alpha);
    pgr.noStroke();
    pgr.textAlign(LEFT);
    pgr.textFont(f10);
    if (!typingInput) pgr.text(value, x+elx + sizex*0.5f-textWidth(str(value))*0.5f, y+ely + sizey*0.5f+3);
    else 
    {
      pgr.text(input, x+elx + sizex*0.5f-textWidth(input)*0.5f, y+ely + sizey*0.5f+3);
      if (frameCount%60>30) //display caret
        try
      {
        pgr.text("|", x+elx + sizex*0.5f-textWidth(input)*0.5f + textWidth(input.substring(0, cursorPos))-2, y+ely + sizey*0.5f+3);
      }
      catch(Exception e)
      {
        //sometimes the caret position gets bugged, no need to crash the whole program for that
      }
    }
    if (sizex != 0) pgr.triangle(x+elx+2, y+ely+2, x+elx+2, y+ely+sizey-2, x+elx+sqrt(2)*(sizex/sizey), y+ely+0.5f*sizey);
    if (pgr != g) pgr.endDraw();
  }

  public void update(float elx, float ely)
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
      value = (0.00001f*pow(-mouseY+y+ely+sizey*0.5f, 3)+(oldValue));
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
          value = PApplet.parseFloat(input);
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
          value = -PApplet.parseFloat(input);
          input = str(value);
        }
        if (key == BACKSPACE) input = input.substring(0, max(0, cursorPos-1)) + input.substring(max(0, cursorPos <= 0 ? 0 : cursorPos--), input.length());
        if (key == DELETE) input = input.substring(0, max(0, cursorPos)) + input.substring(min(input.length(), max(0, cursorPos+1)), input.length());
        if (key == CODED && keyCode == RIGHT) cursorPos = min(++cursorPos, input.length());
        if (key == CODED && keyCode == LEFT) cursorPos = max(0, --cursorPos);
        if (key == CODED && keyCode == UP) 
        {
          value = PApplet.parseFloat(input) + 0.01f;
          input = str(value);
        }
        if (key == CODED && keyCode == DOWN) 
        {
          value = PApplet.parseFloat(input) - 0.01f;
          input = str(value);
        }
        if (key == CODED && keyCode == 33) //page up
        {
          value = PApplet.parseFloat(input) + 1;
          input = str(value);
        }
        if (key == CODED && keyCode == 34) //page down
        {
          value = PApplet.parseFloat(input) - 1;
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

  public void setValue(float input)
  {
    value = input;
  }

  public float getValue()
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

  public void update(float elx, float ely)
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

  public void display(float elx, float ely)
  {
    if (!visible) return;
    if (laserboyMode) fill(color(PApplet.parseInt((sin((elx)/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((ely)+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((x+y)/2+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255)), alpha); 
    else fill(200, alpha);
    noStroke();
    rect(x+elx, y+ely, sizex, sizey);
    if (laserboyMode) fill(color(255-PApplet.parseInt((sin((x)/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((y)+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((elx+ely)/2+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255)), alpha); 
    else fill(50, alpha);
    rect(x+elx, y+ely+map(position, 0, 1, 0, sizey-scrollerSize), sizex, scrollerSize);
  }

  public float getValue()
  {
    return position;
  }

  public void setValue(float input)
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


  public void display(PGraphics pgr, float elx, float ely)
  {
    if (!visible) return;
    if (pgr != g) pgr.beginDraw();
    if (laserboyMode) pgr.fill(color(PApplet.parseInt((sin((y)/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((x)+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((elx+ely)/2+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255)), alpha); 
    else pgr.fill(50, alpha);
    pgr.noStroke();
    pgr.rect(x + elx, y + ely, sizex, sizey);
    if (laserboyMode) pgr.fill(color(PApplet.parseInt((sin((x)/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((y)+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((elx+ely)/2+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255)), alpha); 
    else pgr.fill(255, alpha);
    pgr.textAlign(LEFT);
    pgr.textFont(f10);
    pgr.text(text, x+elx + sizex*0.5f-textWidth(text)*0.5f, y+ely + sizey*0.5f+3);
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

  public void display(PGraphics pgr, float elx, float ely)
  {
    if (!visible) return;
    if (pgr != g) pgr.beginDraw();
    if (isActive)
    {
      if (laserboyMode) pgr.fill(color(PApplet.parseInt((sin((x)/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((y)+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((elx+ely)/2+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255)), alpha); 
      else pgr.fill(127, alpha);
      pgr.stroke(0, alpha);
      pgr.strokeWeight(1);
    } else
    {
      if (laserboyMode) pgr.fill(color(255-PApplet.parseInt((sin((x)/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), 255-PApplet.parseInt((sin((y)+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), 255-PApplet.parseInt((sin((elx+ely)/2+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255)), alpha); 
      else pgr.fill(50, alpha);
      pgr.noStroke();
    }
    pgr.rect(x + elx, y + ely, sizex, sizey);

    if (isActive)
    {
      if (laserboyMode) pgr.fill(color(255-PApplet.parseInt((sin((x)/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), 255- PApplet.parseInt((sin((y)+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), 255-PApplet.parseInt((sin((elx+ely)/2+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255)), alpha); 
      else pgr.fill(0, alpha);
    } else
    {
      if (laserboyMode) pgr.fill(color(PApplet.parseInt((sin((x)/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((y)+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((elx+ely)/2+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255)), alpha); 
      else pgr.fill(255, alpha);
    }

    pgr.textFont(f10);
    pgr.textAlign(CENTER);
    //text(text, x+elx + sizex*0.5-textWidth(text)*0.5, y+ely + sizey*0.5+3);
    pgr.text(text, (int) x+elx + sizex*0.5f, (int) y+ely + sizey*0.5f+3);
    if (pgr != g) pgr.endDraw();
  }

  public void toggle()
  {
    isActive = !isActive;
  }

  public float getValue()
  {
    if (isActive) return 1;
    else return 0;
  }

  public void setValue(float value)
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

  public void toggle()
  {
    dropdown = !dropdown;
  }

  public void display(float elx, float ely)
  {
    if (!visible) return;
    if (laserboyMode) fill(color(PApplet.parseInt((sin((x)/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((y)+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((elx+ely)/2+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255)), alpha); 
    else fill(0, alpha);
    noStroke();
    if (dropdown) triangle(x+elx+sizex-11, y+ely+10, x+elx+sizex-5, y+ely+10, x+elx+sizex-8, y+ely+5);
    else triangle(x+elx+sizex-11, y+ely+10, x+elx+sizex-5, y+ely+10, x+elx+sizex-8, y+ely+15);
    textFont(f10);
    textAlign(LEFT);
    text(text, x+elx+sizex-textWidth(text)-15, y+ely+15);
  }

  public float getValue()
  {
    if (dropdown) return 1;
    else return 0;
  }

  public void setValue(float input)
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

  public void display(float elx, float ely)
  {
    textFont(f10);
    textAlign(LEFT);
    if (laserboyMode) fill(color(PApplet.parseInt((sin((x)/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((y)+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((elx+ely)/2+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255)), alpha); 
    else fill(0, alpha);
    text("X", x+elx, y+ely+10);
  }
}




class OptimizerOverlay extends OverlayImage
{
  Optimizer optimiser;
  int w, h;
  OptimizerOverlay(Optimizer opt, int x, int y, int w, int h)
  {
    super("Optimisation settings", x, y);
    optimiser = opt;
    optimiser.guiVisible = true;
  }

  public void setup()
  {
    size(480, 320, P2D);
    smooth();
    cp5 = new ControlP5(this);

    frame.setSize(width+1, height+1);  //What the actual fuck?! 

    cp5.setFont(f10);
    cp5.enableShortcuts(); //Enables keyboard shortcuts of ControlP5 controls
    cp5.setColorLabel(textcolour);
    cp5.setColorBackground(buttoncolour);
    cp5.setColorActive(activecolour);
    cp5.setColorForeground(mouseovercolour);

    cp5.setBroadcast(false);

    cp5.addToggle("removeCollinear")
      .setPosition(5, 10)
        .setSize(125, 20)
          .setCaptionLabel("Remove collinear points")
            .getCaptionLabel().alignY(CENTER).alignX(CENTER).toUpperCase(false);

    cp5.addToggle("removeSame")
      .setPosition(150, 10)
        .setSize(125, 20)
          .setCaptionLabel("Remove identical points")
            .getCaptionLabel().alignY(CENTER).alignX(CENTER).toUpperCase(false);

    cp5.addToggle("removeBlanked")
      .setPosition(300, 10)
        .setSize(150, 20)
          .setCaptionLabel("Remove stray blanked points")
            .getCaptionLabel().alignY(CENTER).alignX(CENTER).toUpperCase(false);

    cp5.addToggle("interpolateLit")
      .setPosition(5, 50)
        .setSize(125, 20)
          .setCaptionLabel("Interpolate lit points")
            .getCaptionLabel().alignY(CENTER).alignX(CENTER).toUpperCase(false);


    cp5.addSlider("interpolationDistance")
      .setPosition(150, 50)
        .setSize(300, 20)
          .setRange(0.0001f, 1.5f)
            .setCaptionLabel("Interpolation distance")
              .getCaptionLabel().alignY(CENTER).alignX(CENTER).toUpperCase(false);

    cp5.addToggle("interpolateBlanked")
      .setPosition(5, 80)
        .setSize(125, 20)
          .setCaptionLabel("Interpolate blanked points")
            .getCaptionLabel().alignY(CENTER).alignX(CENTER).toUpperCase(false);


    cp5.addSlider("interpolationBlDistance")
      .setPosition(150, 80)

        .setSize(300, 20)
          .setRange(0.0001f, 1.5f)
            .setCaptionLabel("Interpolation distance")
              .getCaptionLabel().alignY(CENTER).alignX(CENTER).toUpperCase(false);


    cp5.addButton("load")
      .setPosition(5, height-75)
        .setSize(125, 20)
          .setCaptionLabel("Load")
            .getCaptionLabel().alignY(CENTER).alignX(CENTER).toUpperCase(false);

    cp5.addButton("save")
      .setPosition(150, height-75)
        .setSize(125, 20)
          .setCaptionLabel("Save")
            .getCaptionLabel().alignY(CENTER).alignX(CENTER).toUpperCase(false);

    cp5.addButton("def")
      .setPosition(295, height-75)
        .setSize(125, 20)
          .setCaptionLabel("Default")
            .getCaptionLabel().alignY(CENTER).alignX(CENTER).toUpperCase(false);

    updateGuiValues();

    cp5.setBroadcast(true);
  }


  public void updateGuiValues()
  {
    cp5.getController("removeCollinear").setValue(optimiser.removeCollinear ? 1f : 0f);
    cp5.getController("removeSame").setValue(optimiser.removeIdentical ? 1f : 0f);
    cp5.getController("removeBlanked").setValue(optimiser.removeBlanked ? 1f : 0f);
    cp5.getController("interpolateLit").setValue(optimiser.interpolateLit ? 1f : 0f);
    cp5.getController("interpolationDistance").setValue(optimiser.interpolationDistance);
    cp5.getController("interpolateBlanked").setValue(optimiser.interpolateBlanked ? 1f : 0f);
    cp5.getController("interpolationBlDistance").setValue(optimiser.interpolateBlDistance);
  }

  public void load()
  {
    optimiser.load();
    updateGuiValues();
  }

  public void save()
  {
    optimiser.save();
  }

  public void def()
  {
    optimiser.def();
    updateGuiValues();
  }

  public void removeCollinear(boolean value)
  {
    optimiser.removeCollinear = value;
  }

  public void removeSame(boolean value)
  {
    optimiser.removeIdentical = value;
  }

  public void removeBlanked(boolean value)
  {
    optimiser.removeBlanked = value;
  }


  public void interpolateLit(boolean value)
  {
    optimiser.interpolateLit = value;
  }

  public void interpolationDistance(float value)
  {
    optimiser.interpolationDistance = value;
  }

  public void interpolateBlanked(boolean value)
  {
    optimiser.interpolateBlanked = value;
  }

  public void interpolationBlDistance(float value)
  {
    optimiser.interpolateBlDistance = value;
  }

  public void draw()
  {

    background(backgroundcolour);
  }

  public void windowClosed()
  {
    optimiser.guiVisible = false;
  }
}


class Optimizer
{
  Frame frame;
  String[] settings = new String[0];
  boolean guiVisible = false;

  boolean removeCollinear = true;
  boolean removeIdentical = true;
  boolean removeBlanked = true;

  boolean interpolateLit = true;
  float interpolationDistance = 1f;

  boolean interpolateBlanked = true;
  float interpolateBlDistance = 1f;

  public void optimise()
  {
    try
    {
      if (removeCollinear || removeIdentical || removeBlanked)
      {
        for (int i = frame.points.size ()-2; i >= 0; i--)
        {
          Point prevp = frame.points.get(i+1);
          Point p = frame.points.get(i);
          Point nextp = null;
          if (i != 0) nextp = frame.points.get(i-1);
          if (prevp == null || p == null || nextp == null) break;

          if (nextp.equals(p)) frame.points.remove(i);
          else
          {

            float xverh = (prevp.position.x-p.position.x)/(nextp.position.x-p.position.x);
            float yverh = (prevp.position.y-p.position.y)/(nextp.position.y-p.position.y);
            float zverh = (prevp.position.z-p.position.z)/(nextp.position.z-p.position.z);
            //println(xverh, yverh, zverh);
            if (xverh == yverh && yverh == zverh) frame.points.remove(i);
          }
        }
      }



      if (interpolateLit || interpolateBlanked)
      {
        float maxdistsqb = sq(interpolateBlDistance*width);
        float maxdistsql = sq(interpolationDistance*width);
        for (int i = frame.points.size ()-2; i >= 0; i--)
        {
          Point prevp = frame.points.get(i+1);
          Point p = frame.points.get(i);
          Point nextp = null;
          if (i != 0) nextp = frame.points.get(i-1);

          float dpsq = (prevp.position.x - p.position.x) * (prevp.position.x - p.position.x) + (prevp.position.y - p.position.y) * (prevp.position.y - p.position.y) + (prevp.position.z - p.position.z) * (prevp.position.z - p.position.z);
          float dnsq = 0;
          if (nextp != null) dnsq = (nextp.position.x - p.position.x) * (nextp.position.x - p.position.x) + (nextp.position.y - p.position.y) * (nextp.position.y - p.position.y) + (nextp.position.z - p.position.z) * (nextp.position.z - p.position.z);


          if ((prevp.blanked && dpsq > maxdistsqb && interpolateBlanked) || (!prevp.blanked && dpsq > maxdistsql && interpolateLit)) {
            float dist = sqrt(dpsq);
            float maxDist = prevp.blanked ? interpolateBlDistance*width : interpolationDistance*width;
            int addedPoints = (int) (dist/maxDist);
            for (int j = 1; j <= addedPoints; j++)
            {
              //println(i, frame.points.size(), j, addedPoints);
              Point newp = new Point(prevp);
              float factor = (1 - (dist - j * maxDist) / dist);
              newp.position.x = prevp.position.x + (p.position.x - prevp.position.x) * factor;
              newp.position.y = prevp.position.y + (p.position.y - prevp.position.y) * factor;
              newp.position.z = prevp.position.z + (p.position.z - prevp.position.z) * factor;
              frame.points.add(i + 1, newp);
            }
          }
        }
      }
    }
    catch(Exception e)
    {
      println(e);
    }
  }

  public void setFrame(Frame frame)
  {
    this.frame = new Frame(frame);
  }

  public Frame getFrame()
  {
    return frame;
  }

  public void setSettings(String[] settings)
  {
    this.settings = settings;
    for (String s : settings)
    {
      String[] sub = splitTokens(s);
      try
      {
        if (sub.length > 1)
        {
          if (sub[0].equals("removeCollinear")) removeCollinear = PApplet.parseBoolean(sub[1]);
          if (sub[0].equals("removeIdentical")) removeIdentical = PApplet.parseBoolean(sub[1]);
          if (sub[0].equals("removeBlanked")) removeBlanked = PApplet.parseBoolean(sub[1]);
          if (sub[0].equals("interpolateLit")) interpolateLit = PApplet.parseBoolean(sub[1]);
          if (sub[0].equals("interpolationDistance")) interpolationDistance = PApplet.parseFloat(sub[1]);
          if (sub[0].equals("interpolateBlanked")) interpolateBlanked = PApplet.parseBoolean(sub[1]);
          if (sub[0].equals("interpolateBlDistance")) interpolateBlDistance = PApplet.parseFloat(sub[1]);
        }
      }
      catch(Exception e)
      {
        status.clear();
        status.add("Error when trying to set optimisation settings.");
      }
    }
    println(settings);
  }

  public String[] getSettingsFile()
  {
    String[] settings = new String[7];
    settings[0] = "removeCollinear " + removeCollinear;
    settings[1] = "removeIdentical " + removeIdentical;
    settings[2] = "interpolateLit " + interpolateLit;
    settings[3] = "interpolationDistance " + interpolationDistance;
    settings[4] = "interpolateBlanked " + interpolateBlanked;
    settings[5] = "interpolateBlDistance " + interpolateBlDistance;
    settings[6] = "removeBlanked " + removeBlanked;



    return settings;
  }

  public void load()
  {
    selectInput("Select an optimisation settings file (.opt)", "optFileSelected");
  }

  public void save()
  {
    String pathname;
    pathname = ".opt";
    File theFile = new File(pathname);
    status.clear();
    status.add("Select where to save the optimisation settings file");
    selectOutput("Select where to save an optimisation settings file", "optFileOutput", theFile);
  }

  public void def()
  {
    boolean removeCollinear = true;
    boolean removeIdentical = true;
    boolean removeBlanked = true;

    boolean interpolateLit = true;
    float interpolationDistance = 1;

    boolean interpolateBlanked = true;
    float interpolateBlDistance = 1;
  }

  public void optFileSelected(File selection)
  {
    if (selection == null)
    {
      status.clear();
      status.add("Optimisation file loading aborted or no valid file selected.");
      return;
    }

    try
    {
      setSettings(loadStrings(selection));
    }
    catch(Exception e)
    {
      status.clear();
      status.add("Error when trying to parse optimisation settings file.");
    }
  }

  public void optFileOutput(File selection)
  {
    if (selection == null)
    {
      status.clear();
      status.add("Optimisation settings not saved.");
      return;
    }
    String location = selection.getAbsolutePath();
    char[] test = new char[4];  //Test if it already has the extension .osc:
    for (int i = 0; i < 4; i++)
    {
      test[i] = location.charAt(i+location.length()-4);
    }
    String testing = new String(test);
    if ( !testing.equals(".opt") )
    {
      location += ".opt";
    }

    saveStrings(location, getSettingsFile());

    status.add("Optimisation settings file was saved to:");
    status.add(location);
  }
}




ArrayList<OverlayImage> overlayImages = new ArrayList<OverlayImage>();


class PFrame extends JFrame
{
  OverlayImage p;
  PFrame(OverlayImage p, int width, int height)
  {
    this.p = p;
    show();
    p.init();
    removeNotify();
    setUndecorated(false);
    addNotify();
    setBounds(100, 100, width, height);
    
    add(p);
    p.frame = this;
    
    
    setTitle(p.title);
    //setResizable(false);
    try
    {
      if (icon == null) 
      {
        icon = createGraphics(256, 256);
        icon.beginDraw();
        icon.image(loadImage("Images/Icon2.png"), 0, 0);
        icon.endDraw();
      }

      setIconImage(icon.image);
    }
    catch(Exception e)
    {
    }
  }

  public void processWindowEvent(WindowEvent object)
  {
    if (object.getID() == WindowEvent.WINDOW_CLOSING)
    {
      p.windowClosed();
      dispose();
    }
  }
}


class OverlayImage extends PApplet
{

  ControlP5 cp5;
  String title;
  float x, y;
  OverlayImage(String name, float x, float y)
  {
    title = name;
    this.x = x;
    this.y = y;
  }


  public void setup()
  {
    cp5 = new ControlP5(this);
  }

  public void draw()
  {
    background(backgroundcolour);
  }

  public void windowClosed()
  {
  }
}

// Fields for Palette Editor tab:

boolean paletteEditor = false;//Palette editor mode
ListBox paletteList;          //ControlP5 selection list 
PalEditor paleditor;
ColorPicker cp;               //ControlP5 colour picker


//      === PALETTE TAB METHODS AND CP5 CALLBACKS ===

// A beginMode() method should always get called upon entering a tab
public void beginPalettes()
{
  paletteEditor = true;    // A mode should always have a boolean set to true in the beginMode() method
  paleditor = new PalEditor();  // A mode preferrably has a class of which an object is initiated in the beginMode() method

  //Rest are specific methods:
  paleditor.setActivePalette(activePalette);
  cp.setColorValue(palettes.get(paleditor.activePal).colours.get(0).getColour());
  cp5.getController("picker-alpha").setVisible(false);
  status.clear();
  status.add("Palette editor entered");
}

// An exitMode() method should always get called upon leaving a tab, while the corresponding boolean is true
public void exitPalettes()
{
  // The mode boolean should be set to false in here
  paletteEditor = false;
  status.clear();
  status.add("Palette editor exited");
}

public void recolourFrames()
{
  for (Frame frame : frames)
  {
    frame.palettePaint(getActivePalette());
  }
}

public void input(String theText) {
  palettes.get(activePalette).name = theText;
  paleditor.updatePaletteList();
}

public void addPalette()
{
  Palette newPalette = new Palette();
  newPalette.name = " ";
  palettes.add(newPalette);
  paleditor.activePal = palettes.size()-1;
  activePalette = palettes.size()-1;
  paleditor.updatePaletteList();
}

public void removePalette()
{
  if (palettes.size() > 1)
  {
    String thename = palettes.get(activePalette).name;
    palettes.remove(activePalette);
    status.clear();
    status.add("Palette " + thename + " removed.");
  } else 
  {
    status.clear(); 
    status.add("Can't remove all palettes");
  }
  if (activePalette >=palettes.size() ) 
  {
    activePalette = palettes.size()-1;
    paleditor.activePal = palettes.size()-1;
  }
  paleditor.updatePaletteList();
}

public void importPalette()
{
  status.clear();
  selectInput("Select an image file to load in as a palette", "paletteSelected");
}

public void exportPalette()
{
  String pathname = trim(palettes.get(activePalette).name) + ".png";
  File theFile = new File(pathname);
  selectOutput("Select a file to save a palette as image", "paletteOutput", theFile);
}

public void paletteOutput(File selection)
{
  if (selection == null)
  {
    status.clear();
    status.add("Palette export aborted.");
    return;
  }
  String location = selection.getAbsolutePath();
  char[] test = new char[4];
  for (int i = 0; i < 4; i++)
  {
    test[i] = location.charAt(i+location.length()-4);
  }
  String testing = new String(test);
  if ( !testing.equals(".png") )
  {
    location += ".png";
  }
  PImage img = paleditor.getPaletteAsImage();
  img.save(location);

  status.clear(); 
  status.add("Palette succesfully exported on location:");
  status.add(location);
}

public void paletteSelected(File selection) {
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
    status.add("Loading palette " + selection.getAbsolutePath());
    try {
      img = loadImage(selection.getAbsolutePath());
    }
    catch(Exception e)
    {
      status.add("Invalid file");
      return;
    }
    String fixName = selection.getName();
    char[] fix = new char[fixName.length()];
    for (int i = 0; i < fixName.length (); i++)
    {
      fix[i] = fixName.charAt(i);
    }

    String fixedName = fixName;    
    if ( fix[fixName.length()-4] == '.' && (fix[fixName.length()-3] == 'p' || fix[fixName.length()-3] == 'j') && (fix[fixName.length()-2] == 'n' || fix[fixName.length()-2] == 'p')&& fix[fixName.length()-1] == 'g' )
    {
      String[] outOfNames = split(fixName, '.');
      fixedName = outOfNames[0];
    }
    paleditor.importImageAsPalette(img, fixedName);
    paleditor.activePal = palettes.size()-1;
    activePalette = palettes.size()-1;
  }
}

public void numberOfColours(String theText)
{
  int maxNum = PApplet.parseInt(theText);
  if (maxNum > 256) maxNum = 256;
  if (maxNum < 0) maxNum = 0;
  palettes.get(activePalette).resizePalette(maxNum);

  status.clear();
  status.add("Resized palette "+palettes.get(activePalette).name+" to " + maxNum + " colours.");
}


class PalEditor
{

  int activePal = 0;     //There is already an activePalette global field, maybe this one was a bit redundant.
  int activeCol = 0;     //The colour that was being selected with the mouse
  boolean dontChangeColourMoron=false;    //Getting frustrated when bug fixing

  PalEditor()
  {
    updatePaletteList();
  }

  public void update()
  {
    if (activePal >= 0  || activePal < palettes.size())
    {
      displayPalette(palettes.get(activePal));
    }
    fill(255);
    text(palettes.get(activePal).name, width-150, 460);
  }

  public void updatePaletteList()
  {
    paletteList.clear();
    int i = 0;
    for (Palette palette : palettes)
    {
      paletteList.addItem(palette.name, i++);
    }
  }

  public PImage getPaletteAsImage()
  {
    PImage pg = createImage(palettes.get(activePal).colours.size(), 1, RGB);
    for (int i = 0; i < palettes.get (activePal).colours.size(); i++)
    {
      pg.pixels[i] = palettes.get(activePal).colours.get(i).getColour();
    }
    return pg;
  }

  public void importImageAsPalette(PImage img, String theName)
  {
    if (img == null)
    {
      status.add("Failed to load palette " + theName);
      return;
    }
    Palette palette = new Palette(theName);

    for ( int i = 0; i < min (img.width, 256); i++)
    {
      if (palette.colours.size() < 256) palette.addColour(img.get(i, 0));
    }
    palettes.add(palette);
    activePal = palettes.size()-1;
    status.add("Palette " + palette.name + " loaded.");
    updatePaletteList();
  }

  //A bit of a bad name, should be called "select colour"
  //It checks if you clicked on a colour square in the palette display
  //then sets that colour active
  public void editPalette(int x, int y)
  {
    int activeX = -1;
    int activeY = -1;
    for (int i = 0; i < 16; i++)
    {
      if (x >= width-425+25*i && x <= width-425+25*i+20)
      {
        activeX = i;
      }
    }

    for (int i = 0; i < 16; i++)
    {
      if (y >= 25+25*i && y <= 25+25*i +20)
      {
        activeY = i;
      }
    }

    if ( activeX == -1 || activeY == -1)
    {
      return;
    } else
    {
      int activeColr = activeX + activeY*16;
      if (activeColr >= 0 && activeColr < palettes.get(activePal).colours.size()) 
      {
        dontChangeColourMoron=true;
        cp.setColorValue(palettes.get(activePal).colours.get(activeColr).getColour());
        dontChangeColourMoron=false;
        activeCol = activeColr;
      }
    }
  }

  public void setActiveColour(int colour)
  {
    if (activePal >= 0 && activePal < palettes.size() )
    {
      if (activeCol >= 0 && activeCol < palettes.get(activePal).colours.size() )
      {
        //int alpha = (colour >> 24) & 0xff;
        //int red = (colour >> 16) & 0xFF;
        //int green = (colour >> 8) & 0xFF;
        //int blue = colour & 0xFF;
        //color colr = color( alpha*red, alpha *green, alpha*blue);
        if (!dontChangeColourMoron) palettes.get(activePal).colours.get(activeCol).changeColour(colour);
      }
    }
  }


  public void setActivePalette(int ind)
  {
    activePal = ind;
  }

  public void displayPalette(Palette palette)
  {
    int i = 0;
    for (PaletteColour colour : palette.colours)
    {
      strokeWeight(0);
      if (i == activeCol) 
      {
        int c = color( PApplet.parseInt(sin(PApplet.parseFloat(frameCount)/5)*127+128), 50, 50);
        stroke(c);
        strokeWeight(5);
      } else
      {
        stroke( color(50, 50, 50));
        strokeWeight(1);
      }
      colour.displayColour(width-425+25*(i%16), 25+25*PApplet.parseInt(i/16), 20);
      i++;
    }
  }
}


class Palette
{

  /*
   * A Palette is some horrible contraption that stores colours.
   * Unfortunately the majority of ilda files needs them.
   */

  String name;
  String companyName;
  int totalColors;
  int paletteNumber;
  int scannerHead;
  StringList hoofding; //Data now in a nice StringList to display 

  ArrayList<PaletteColour> colours;     //A list of PaletteColours. Not sure what you expected in a Palette class.

  Palette()
  {
    hoofding = new StringList();
    colours = new ArrayList<PaletteColour>();
  }

  Palette(Palette palette)
  {
    this.name = new String(palette.name);
    this.companyName = new String(palette.companyName);
    this.totalColors = palette.totalColors;
    this.paletteNumber = palette.paletteNumber;
    this.scannerHead = palette.scannerHead;
    this.colours = new ArrayList<PaletteColour>(palette.colours);
  }

  Palette(String _name)
  {
    hoofding = new StringList();
    name = _name;
    colours = new ArrayList<PaletteColour>();
    if (name.equals("Random") )
    {
      for (int i = 0; i < 256; i++)
      {
        PaletteColour paletteColour = new PaletteColour();
        colours.add(paletteColour);
      }
      companyName = "IldaView";
    }
  }

  public byte[] paletteToBytes()
  {

    ArrayList<Byte> theBytes;
    theBytes = new ArrayList<Byte>();

    theBytes.add(PApplet.parseByte('I'));       //Bytes 1-4: "ILDA"
    theBytes.add(PApplet.parseByte('L'));
    theBytes.add(PApplet.parseByte('D'));
    theBytes.add(PApplet.parseByte('A'));
    theBytes.add(PApplet.parseByte(0));         //Bytes 5-8: Format Code 2
    theBytes.add(PApplet.parseByte(0));
    theBytes.add(PApplet.parseByte(0));
    theBytes.add(PApplet.parseByte(2));



    for (int i = 0; i < 8; i++)    //Bytes 9-16: Name
    {
      char letter;
      if (name.length() < i+1) letter = ' ';
      else letter = name.charAt(i);
      theBytes.add(PApplet.parseByte(letter));
    }



    if (companyName == null)   //Bytes 17-24: Company Name
    {
      theBytes.add(PApplet.parseByte('I'));     //If empty: call it "IldaView"
      theBytes.add(PApplet.parseByte('l'));
      theBytes.add(PApplet.parseByte('d'));
      theBytes.add(PApplet.parseByte('a'));
      theBytes.add(PApplet.parseByte('V'));
      theBytes.add(PApplet.parseByte('i'));
      theBytes.add(PApplet.parseByte('e'));
      theBytes.add(PApplet.parseByte('w'));
    }
    else
    {
      for (int i = 0; i < 8; i++)  
      {
        char letter;
        if (companyName.length() < i+1) letter = ' ';
        else letter = companyName.charAt(i);
        theBytes.add(PApplet.parseByte(letter));
      }
    }

    int totalSize = colours.size();
    if (totalSize < 1) return null;
    if (totalSize > 255) totalSize = 256;

    theBytes.add(PApplet.parseByte((totalSize>>8) & 0xff));              //Bytes 25-26: total colours 
    theBytes.add(PApplet.parseByte(totalSize&0xff)); //Limited to 256 so byte 25 is redundant


    //Bytes 27-28: Palette number (just return activePalette)
    theBytes.add(PApplet.parseByte((activePalette>>8) & 0xff));    //This better be correct
    theBytes.add(PApplet.parseByte(activePalette & 0xff));

    theBytes.add(PApplet.parseByte(0));    //Bytes 29-30: Future
    theBytes.add(PApplet.parseByte(0));
    theBytes.add(PApplet.parseByte(scannerHead)); //Byte 31: Scanner head
    theBytes.add(PApplet.parseByte(0));    //Also Future



    for (int i = 0; i < min(256, colours.size()); i++)    //Rest: colour data
    {
      PaletteColour colour = colours.get(i);
      theBytes.add(PApplet.parseByte(colour.getRed()));
      theBytes.add(PApplet.parseByte(colour.getGreen()));
      theBytes.add(PApplet.parseByte(colour.getBlue()));
    }

    byte [] bt = new byte[theBytes.size()];
    for (int i = 0; i<theBytes.size(); i++)
    {
      bt[i] = theBytes.get(i);
    }

    return bt;
  }

  public void resizePalette(int newSize)
  {
    //Delete all colours that are above the new size:
    if (newSize < colours.size() && newSize >=0)
    {
      int blargh = colours.size();
      for (int i = newSize; i < blargh; i++)
      {
        colours.remove(newSize);
      }
    }

    //Add new random colours if the new size is bigger than the previous one:
    else
    {
      int blargh = colours.size();
      for (int i = blargh; i < newSize; i++)
      {
        PaletteColour newColour = new PaletteColour();
        colours.add(newColour);
      }
    }
  }

  public void addColour(int r, int g, int b)
  {
    PaletteColour theColour = new PaletteColour(r, g, b);
    colours.add(theColour);
  }

  public void addColour(int colour)
  {
    PaletteColour theColour = new PaletteColour(colour);
    colours.add(theColour);
  }

  public PaletteColour getPaletteColour(int index)
  {
    if (index < colours.size() && index >= 0)
    {
      return colours.get(index);
    }
    else
    {
      println("Invalid colour");
      return null;
    }
  }

  public int getColour(int index)
  {
    if ( index < colours.size() && index >= 0)
    {
      return colours.get(index).getColour();
    }
    else
    {

      return color(0, 0, 0);
    }
  }

  public void formHeader()
  {
    hoofding.clear();
    hoofding.append("Frame: " + name);
    hoofding.append("Company: " + companyName);
    hoofding.append("Amount of colours: " + totalColors);
    hoofding.append("Palette number: " + paletteNumber);
    hoofding.append("Scanner head: " + scannerHead);
  }
}


class PaletteColour
{
  /*
   * "Color" is not a Processing class, but is a protected name, so we had to find an alternative!
   */

  int red;
  int green;
  int blue;
  int yellow;
  int cyan;
  int dblue;

  PaletteColour()
  {
    red = PApplet.parseInt(random(0, 255));
    green = PApplet.parseInt(random(0, 255));
    blue = PApplet.parseInt(random(0, 255));
  }

  PaletteColour(int r, int g, int b)
  {
    red = r;
    green = g;
    blue = b;
  }

  PaletteColour(int r, int g, int b, int y, int c, int db)
  {
    red = r;
    green = g;
    blue = b;
    yellow = y;
    cyan = c;
    dblue = db;
  }

  PaletteColour( int colour)
  {
    red = (colour >> 16) & 0xFF;  // Faster way of getting red(argb)
    green = (colour >> 8) & 0xFF;   // Faster way of getting green(argb)
    blue = colour & 0xFF;          // Faster way of getting blue(argb)
  }


  public void changeColour(int colour)
  {
    //int a = (colour >> 24) & 0xFF;
    red = (colour >> 16) & 0xFF;  // Faster way of getting red(argb)
    green = (colour >> 8) & 0xFF;   // Faster way of getting green(argb)
    blue = colour & 0xFF;          // Faster way of getting blue(argb)
  }

  public void displayColour(int x, int y, int size)
  {
    fill(color(red, green, blue));
    rect(x, y, size, size);
  }

  public int getRed()
  {
    return red;
  }

  public int getGreen()
  {
    return green;
  }

  public int getBlue()
  {
    return blue;
  }

  public int getYellow()
  {
    return yellow;
  }

  public int getCyan()
  {
    return cyan;
  }

  public int getDarkBlue()
  {
    return dblue;
  }

  public int getColour()
  {
    return color(red, green, blue);
  }
}


class Point
{

  /* 
   * Points are always 3D and with RGB colour variables.
   */

  PVector position;
  int colour;
  boolean blanked;
  int paletteIndex;


  Point( PVector _position, int red, int green, int blue, boolean _blanked)
  {
    position = _position;
    colour = color(red, green, blue);
    blanked = _blanked;
    //palette = false;
  }
  Point(float x, float y, float z, int red, int green, int blue, boolean _blanked)
  {
    position = new PVector(x, y, z);
    colour = color(red, green, blue);
    blanked = _blanked;
    //palette = false;
  }

  Point(float x, float y, float z, int _paletteIndex, boolean _blanked)
  {
    position = new PVector(x, y, z);
    paletteIndex = _paletteIndex;
    blanked = _blanked;
    //palette = true;
  }

  //        !!! Notice how this constructor is different from the previous ones, 
  //            this is because "color" is actually an int and otherwise 
  //            there would be two Point(PVector,int,boolean) constructors...

  Point(PVector _position, boolean _blanked, int _paletteIndex)
  {
    position = _position;
    paletteIndex = _paletteIndex;
    blanked = _blanked;
    //palette = true;
  }

  Point(int x, int y, int z, int _paletteIndex, boolean _blanked)
  {
    position = new PVector(x, y, z);
    paletteIndex = _paletteIndex;
    blanked = _blanked;
  }

  Point(float x, float y, float z, boolean _blanked, int theColour)
  {
    position = new PVector(x, y, z);
    colour = theColour;
    blanked = _blanked;
    //palette = false;
  }

  Point(Point point)
  {
    this.position = new PVector(point.position.x, point.position.y, point.position.z);
    this.colour = point.colour;
    this.blanked = point.blanked;
    this.paletteIndex = point.paletteIndex;
  }

  public Point clone()
  {
    Point point = new Point(this);
    return point;
  }

  public Point clone(Point point)
  {
    Point newPoint = new Point(point);
    return newPoint;
  }

  public void displayPoint()
  {
    strokeWeight(3);
    stroke(colour);
    if (blanked) stroke(75, 75, 75);
    point(position.x, position.y, position.z);
  }

  public int getBestFittingPaletteColourIndex(Palette palette)
  {
    int index = 0;
    float distance = 1000;
    PVector colourPos = new PVector((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF);
    int i = 0;
    for (PaletteColour theColour : palette.colours)
    {
      PVector palColourPos = new PVector(theColour.red, theColour.green, theColour.blue);
      float d = colourPos.dist(palColourPos);
      if ( d < distance)
      {
        distance = d;
        index = i;
      }
      i++;
    }
    return index;
  }

  public PVector getPosition()
  {
    return position;
  }

  public void setColourFromPalette(Palette palette)
  {
    setColourFromPalette(palette, paletteIndex);
  }

  public void setColourFromPalette(Palette palette, int index)
  {
    colour = palette.getColour(index);
  }


  public boolean equals(Point p)
  {
    return position.x == p.position.x && position.y == p.position.y && position.z == p.position.z && colour == p.colour && paletteIndex == p.paletteIndex && p.blanked == blanked;
  }
}


/*
 * Originally the Sequence Creator had different modes so you could pick one to create frames
 * but only two got implemented: oscillabstract and deluxe paint
 * now only oscillabstract remains and deluxe paint mode is present as an easter egg
 */



// Fields for Sequence Creator tab:

RadioButton seqMode;         //Pick a mode
boolean seqcreator = false;  //Deluxe Paint mode
int creatorMode = -1;        //Which mode is currently active
SequenceCreator sequenceCreator;
IntList cheats = new IntList();
int cheatPos = 0;

//      === SEQUENCE CREATOR TAB METHODS AND CP5 CALLBACKS ===

//Enter the Sequence Creator tab:
public void sequenceCreator()
{
  seqcreator = true;
  if (sequenceCreator == null) sequenceCreator = new SequenceCreator();
  oscillAbstract();
  if (creatorMode != 1) creatorMode = 1;
}

//Exit the Sequence Creator tab:
public void exitSequenceCreator()
{
  seqcreator = false;
}

//Click the Import button:
public void finishSeqCreator()
{
  if (sequenceCreator != null) frames.addAll(sequenceCreator.getFrames());
  exitSequenceCreator();

  cp5.getWindow().activateTab("default");
  enterDefaultTab();

  cp5.getController("finishSeqCreator").setMouseOver(false);
}

//Clicked the clear button:
public void clearSeqCreatorFrames()
{
  if (sequenceCreator != null) sequenceCreator.clearFrames();
}

public void showSQBlanking(boolean theBlank)
{
  showBlanking = theBlank;
}

public void initiateSeqCreatorMode(int mode)
{
  if (sequenceCreator != null) 
  {
    if (mode != creatorMode && mode != -1)
    {
      switch(creatorMode)
      {
      case 0 : 
        exitDeluxePaint();
        break;
      case 1 : 
        exitOscillabstract();
        break;
      }
      switch (mode)
      {
      case 0 : 
        deluxePaint();
        break;
      case 1 : 
        oscillAbstract();
        break;
      }
      creatorMode = mode;
    }
  }
}


//SequenceCreator class:
//I guess the reason this is a class is that it doesn't use the memory when it's not used
//hell if I knew
class SequenceCreator
{
  //The two modes, as a relict of the time more were going to be implemented
  DeluxePaint deluxe;    
  Oscillabstract osc;

  int numberOfFrames;

  SequenceCreator()
  {
    numberOfFrames = 20;
    creatorMode = 1;
    cheats.clear();
    cheats.append(UP);
    cheats.append(UP);
    cheats.append(DOWN);
    cheats.append(DOWN);
    cheats.append(LEFT);
    cheats.append(RIGHT);
    cheats.append(LEFT);
    cheats.append(RIGHT);
    cheats.append((int) 'b');
    cheats.append((int) 'a');
  }

  public void update()
  {
    switch(creatorMode)
    {
    case 0 : 
      if (deluxe != null) deluxe.update();
      break;
    case 1 :
      if (osc != null) osc.update();
      break;
    }

    if (keyHit)
    {
      if (cheatPos < 8 && key == CODED)
      {
        if (keyCode == cheats.get(cheatPos)) cheatPos++;
        else cheatPos = 0;
      } else 
      {
        if (key == cheats.get(cheatPos)) cheatPos++;
        else cheatPos = 0;
      }
      if (cheatPos == 10) 
      {
        initiateSeqCreatorMode(0);
        cheatPos = 0;
      }
    }
  }

  public void update3D()
  {
    /*
    switch(creatorMode)
     {
     case 0 : 
     //if (deluxe != null) deluxe.update();
     break;
     case 1 : 
     break;
     }
     */
  }

  public void clearFrames()
  {
    if (creatorMode == 0)
    {
      for (Frame frame : deluxe.theFrames)
      {
        frame.points.clear();
      }
    }
  }

  public ArrayList<Frame> getFrames()
  {
    ArrayList<Frame> theFrames = new ArrayList<Frame>();
    switch(creatorMode)
    {
    case 0: 
      theFrames = deluxe.theFrames;
      break;
    case 1:
      theFrames = osc.outFrames;


      for (int i = 0; i < theFrames.size (); i++)
      {
        Frame frame = theFrames.get(i);
        frame.ildaVersion = 4;
        frame.frameName = "Oscillabstract";
        frame.companyName = "IldaViewer";
        frame.pointCount = frame.points.size();
        frame.frameNumber = i;
        frame.totalFrames = theFrames.size();
        frame.scannerHead = 123;
        frame.formHeader();
      }

      break;
    }
    return theFrames;
  }

  //Forward the Mouse event:
  public void mousePressed()
  {
    if (creatorMode == 1)
    {
      if (osc != null) osc.mousePressed();
    }
  }
}

//           === DELUXE PAINT ===

public void deluxePaint()
{
  cam.setActive(false);
  sequenceCreator.deluxe = new DeluxePaint();
  cp5.getController("showSQBlanking").setVisible(true);
  cp5.getController("clearSeqCreatorFrames").setVisible(true);

  status.clear();
  status.add("Mode Deluxe Paint entered. Drag around with the mouse.");
}

public void exitDeluxePaint()
{
  frameRate(60);

  if (frames.size() > 1) cp5.getGroup("framePlayer").setVisible(true);
  if (frames.size()>0)
  {
    cp5.getController("clearFrames").setVisible(true);
    cp5.getController("showBlanking").setVisible(true);
    cp5.getController("showBlanking").setValue(PApplet.parseFloat(PApplet.parseInt(showBlanking)));
  } else
  {
    cp5.getController("clearFrames").setVisible(false);
    cp5.getController("showBlanking").setVisible(false);
    cp5.getController("showBlanking").setValue(PApplet.parseFloat(PApplet.parseInt(showBlanking)));
  }

  status.clear();
  status.add("Mode Deluxe Paint exited.");
}


class DeluxePaint
{

  /*
   * Inspired by http://hamoid.tumblr.com/post/65383946931/a-small-loop-drawing-processing-program-inspired
   */


  float redPhase = random(0, TWO_PI);      //Colour modulation parameters
  float greenPhase = random(0, TWO_PI);
  float bluePhase = random(0, TWO_PI);
  float redFreq = random(0.1f, 2);
  float greenFreq = random(0.1f, 2);
  float blueFreq = random(0.1f, 2);

  ArrayList<Frame> theFrames = new ArrayList<Frame>();

  boolean firstpoint = true;

  //Constructor
  DeluxePaint()
  {
    for (int i = 0; i < sequenceCreator.numberOfFrames; i++)
    {
      Frame aFrame = new Frame();
      aFrame.ildaVersion = 5;
      aFrame.frameName = "Frame " + i;
      aFrame.companyName = "IldaView";
      aFrame.frameNumber = i+1;
      aFrame.totalFrames = sequenceCreator.numberOfFrames;
      aFrame.scannerHead = 9000;
      theFrames.add(aFrame);
    }

    firstpoint = true;  //The CP5 mouseover check method is apparently not very accurate?

    frameRate(120);
  }


  //Not entirely unlike draw()
  public void update()
  {
    if (!theFrames.isEmpty())
    {

      int currFrame = frameCount % sequenceCreator.numberOfFrames;
      if (mousePressed )
      {

        if (cp5.getWindow().getMouseOverList().isEmpty() && !firstpoint)
        {
          Frame theFrame = theFrames.get(currFrame);
          int colour = color(PApplet.parseInt((sin(PApplet.parseFloat(theFrame.points.size())*redFreq/sequenceCreator.numberOfFrames+redPhase)*0.5f+0.5f)*255), PApplet.parseInt((sin(PApplet.parseFloat(theFrame.points.size())*greenFreq/sequenceCreator.numberOfFrames+greenPhase)*0.5f+0.5f)*255), PApplet.parseInt((sin(PApplet.parseFloat(theFrame.points.size())*blueFreq/sequenceCreator.numberOfFrames+bluePhase)*0.5f+0.5f)*255));
          theFrame.points.add(new Point(min(max(0, pmouseX), width), min(max(0, pmouseY), height), 0, true, colour ));    //Notice the slightly different constructor!
          theFrame.points.add(new Point(min(max(0, mouseX), width), min(max(0, mouseY), height), 0, false, colour ));
          theFrame.pointCount = theFrame.points.size();
        }

        if (firstpoint) firstpoint = false;
      }
      theFrames.get(currFrame).drawFrame(showBlanking);
    }
  }
}


//            === OSCILLABSTRACT ===

public void oscillAbstract()
{
  if (sequenceCreator.osc == null) sequenceCreator.osc = new Oscillabstract();
  cp5.getController("showSQBlanking").setVisible(false);  
  cp5.getController("clearSeqCreatorFrames").setVisible(false);  
  cp5.getController("emptyOscElements").setVisible(true);
  cp5.getController("loadElements").setVisible(true);
  cp5.getController("saveElements").setVisible(true);

  status.clear();
  status.add("Mode Oscillabstract entered. Click to add an element and connect the nodes by dragging.");
}

public void exitOscillabstract()
{
  cp5.getController("emptyOscElements").setVisible(false);
  cp5.getController("loadElements").setVisible(false);
  cp5.getController("saveElements").setVisible(false);
}

public void emptyOscElements()
{
  cp5.getController("emptyOscElements").setLabel("Confirm... (enter)");
  sequenceCreator.osc.resetWorkspace = true;
  status.clear();
  status.add("Confirm resetting by pressing Enter. This will remove all elements and connections.");
}

//Load workspace cascade:
public void loadElements()
{
  thread("loadOscWorkspace");
}

public void loadOscWorkspace()
{
  status.clear();
  status.add("Warning: this will overwrite the current workspace!");
  /*
  if (!sequenceCreator.osc.unsd.inStack)
   {
   overlay.addImage(sequenceCreator.osc.unsd);
   }
   */
  selectInput("Load a workspace (.osc file)", "oscFileLoad");
}

public void oscFileLoad(File selection)
{
  status.clear();
  String[] input;

  //Checks:
  if (selection == null)
  {
    status.add("Error when trying to read file:" );
    status.add("Aborted import or file does not exist.");
    return;
  }

  sequenceCreator.osc.openedLocation = selection;

  try
  {
    input = loadStrings(selection);
  }
  catch(Exception e)
  {
    status.add("Error when trying to read file " + selection.getAbsolutePath());
    status.add("Could not read file");
    return;
  }

  if (input.length<6 || !input[0].equals("Oscillabstract workspace") || !(splitTokens(input[1])[0].equals("IldaViewer")) || !(splitTokens(input[2])[1].equals("colouredmirrorball")))
  {
    status.add("Error when trying to read file " + selection.getAbsolutePath());
    status.add("Invalid, modified or unsupported file");
    return;
  }

  //Seems like a correct file, let's load it in!

  //Clear the workspace before adding elements:
  sequenceCreator.osc.elements.clear();
  sequenceCreator.osc.connections.clear();

  //Loop through the text file and search for key words:
  for (int i = 6; i < input.length; i++)
  {
    String arg = input[i];
    String[] blah = splitTokens(arg);
    if (blah.length > 0)
    {
      if (blah[0].equals("Element")) 
      {
        String name = "";
        //We're dealing with an Element here, skip ahead and search for the end of this Element section:
        for (int j = i+1; j < input.length; j++)
        {

          String[] test = splitTokens(input[j]);

          boolean create = false;
          if (test.length > 0)
          {
            if (test[0].equals("Element") || test[0].equals("Connection") )  //Element and Connection are two key words, so let's create an element later on!
            {
              create = true;
            }
            if (test[0].equals("Name")) name = test[1];    //we need the name soon
            if (test[0].equals("Index"))    //This is important, the currentIndex value is used for identifying the elements so make sure it has a different value
            {
              int index = PApplet.parseInt(test[1]);
              if (index >= sequenceCreator.osc.currentIndex) sequenceCreator.osc.currentIndex = index + 1;
            }
          }
          if (j == input.length-1) create = true;
          if (create)
          {
            StringList its3AmAndIHaveClassTomorrowMorningButImFixingThisAnyway = new StringList();
            for (int k = 0; k < j-i; k++)
            {
              if (input[k+i] != null) its3AmAndIHaveClassTomorrowMorningButImFixingThisAnyway.append(input[k+i]);
            }

            String[] elArg = its3AmAndIHaveClassTomorrowMorningButImFixingThisAnyway.array();                  //  <-----   Add new elements here!
            if (name.equals("Source")) sequenceCreator.osc.elements.add(new Oscisource(elArg));
            if (name.equals("Output")) sequenceCreator.osc.elements.add(new Oscilloutput(elArg));
            if (name.equals("Merge")) sequenceCreator.osc.elements.add(new Oscimerger(elArg));
            if (name.equals("Breakout")) sequenceCreator.osc.elements.add(new Oscibreakout(elArg));
            if (name.equals("Breakin")) sequenceCreator.osc.elements.add(new Oscibreakin(elArg));
            if (name.equals("Segment")) sequenceCreator.osc.elements.add(new Oscisegment(elArg));
            if (name.equals("Clip")) sequenceCreator.osc.elements.add(new Osciclip(elArg));
            if (name.equals("RGB")) sequenceCreator.osc.elements.add(new Oscicolour(elArg));
            if (name.equals("Palette")) sequenceCreator.osc.elements.add(new Oscipalette(elArg));
            if (name.equals("Palettifier")) sequenceCreator.osc.elements.add(new Oscipalettifier(elArg));
            if (name.equals("Buffershift")) sequenceCreator.osc.elements.add(new Oscibuffershift(elArg));
            if (name.equals("RGB2HSB")) sequenceCreator.osc.elements.add(new OsciRGB2HSB(elArg));
            if (name.equals("XYZ2RThetaPhi")) sequenceCreator.osc.elements.add(new OsciXYZ2RThetaPhi(elArg));
            if (name.equals("Translate")) sequenceCreator.osc.elements.add(new Oscitranslate(elArg));
            if (name.equals("Rotate")) sequenceCreator.osc.elements.add(new Oscirotate(elArg));
            if (name.equals("Scale")) sequenceCreator.osc.elements.add(new Osciscale(elArg));
            if (name.equals("Oscillator")) sequenceCreator.osc.elements.add(new Oscillator(elArg));
            if (name.equals("Constant")) sequenceCreator.osc.elements.add(new Oscilloconstant(elArg));
            if (name.equals("Adder")) sequenceCreator.osc.elements.add(new Oscilladder(elArg));
            if (name.equals("Multiplier")) sequenceCreator.osc.elements.add(new Osciplier(elArg));
            if (name.equals("Math")) sequenceCreator.osc.elements.add(new Oscimath(elArg));
            if (name.equals("Logic")) sequenceCreator.osc.elements.add(new Oscilogic(elArg));
            if (name.equals("Clock")) sequenceCreator.osc.elements.add(new Oscilloclock(elArg));
            if (name.equals("Inspect")) sequenceCreator.osc.elements.add(new Oscinspect(elArg));
            if (name.equals("Optimise")) sequenceCreator.osc.elements.add(new Osciptimize(elArg));
            j = input.length;
          }
        }
      }

      if (blah[0].equals("Connection"))
      {
        //This is a Connection, see where its section ends:
        for (int j = i+1; j < input.length; j++)
        {
          String[] test = splitTokens(input[j]);
          boolean create = false;
          if (test.length > 0)
          {
            if (test[0].equals("Element") || test[0].equals("Connection") )
            {
              create = true;
            }
          }
          if (j == input.length-1) create = true;
          if (create)
          {
            StringList its3AmAndIHaveClassTomorrowMorningButImFixingThisAnyway = new StringList();
            for (int k = 0; k < j-i; k++)
            {
              if (input[k+i] != null) its3AmAndIHaveClassTomorrowMorningButImFixingThisAnyway.append(input[k+i]);
            }
            String[] connArg = its3AmAndIHaveClassTomorrowMorningButImFixingThisAnyway.array();
            sequenceCreator.osc.connections.add(new Connection(connArg));
            j = input.length;
          }
        }
      }
    }
  }

  status.add("Loaded in workspace:");
  status.add(selection.getAbsolutePath());
}

//Save workspace cascade:
public void saveElements()
{
  thread("saveOscWorkspace");
}

public void saveOscWorkspace()
{
  String pathname;
  pathname = ".osc";
  File theFile = new File(pathname);
  status.clear();
  status.add("Select where to save the workspace");
  selectOutput("Save the current workspace...", "oscFileSave", theFile);
}

public void oscFileSave(File selection)
{
  if (selection == null)
  {
    status.clear();
    status.add("Workspace not saved.");
    return;
  }
  status.clear();

  sequenceCreator.osc.openedLocation = selection;    //Save this so it knows where to save the ilda files

  String location = selection.getAbsolutePath();
  char[] test = new char[4];  //Test if it already has the extension .osc:
  for (int i = 0; i < 4; i++)
  {
    test[i] = location.charAt(i+location.length()-4);
  }
  String testing = new String(test);
  if ( !testing.equals(".osc") )
  {
    location += ".osc";
  }


  StringList output = new StringList();  //This is the actual StringList that will get written to the file

  //Add some junk in the beginning:
  output.append("Oscillabstract workspace");
  output.append("IldaViewer version " + ildaViewerVersion);
  output.append("Author: colouredmirrorball");
  output.append("Visit http://www.photonlexicon.com/forums/showthread.php/21601-Another-Ilda-view-tool");
  output.append("Modify this file at your own risk.");
  output.append("");
  output.append("*************************************************************************************");
  output.append("");
  output.append("");

  //Retrieve the state of each Element as a String[]
  for (Oscillelement element : sequenceCreator.osc.elements)
  {
    output.append("Element");
    String[] elementStrings = element.getElementAsString();  //A Source will also export its content as an ilda file
    for (String arg : elementStrings)
    {
      output.append(arg);
    }
    output.append("");
  }

  //Retrieve the state of each Connection as a String[]
  for (Connection connection : sequenceCreator.osc.connections)
  {
    output.append("Connection");
    String[] connectionString = connection.getConnectionAsString();
    for (String arg : connectionString)
    {
      output.append(arg);
    }
    output.append("");
  }

  saveStrings(location, output.array());

  status.add("Workspace was saved to:");
  status.add(location);
}

boolean checkMouseOver = true;
boolean deleteOscillelement = false;
float   deletingElementIndex = -1;

//I apologise in advance for what follows. Try to keep up!

class Oscillabstract
{
  ArrayList<Oscillelement> elements = new ArrayList<Oscillelement>();
  NewElement theElement;    //This Element is the new Element dialog that pops up when you click somewhere
  boolean addNewElement = false;  //accompagnying booleans
  boolean showAddDialog = true;
  int dialogX;      //position of new element dialog
  int dialogY;
  boolean hideElements = false;    //Should be set to true if a big dialog "window" (= rectangle) is active to avoid overlap
  int currentIndex = 0;
  ArrayList<Connection> connections = new ArrayList<Connection>();
  ArrayList<Frame> outFrames = new ArrayList<Frame>();
  boolean resetWorkspace = false;
  File openedLocation;      //for saving/loading ilda files
  boolean dragWorkspace = false;
  boolean unsaved = false;
  //UnsavedDialog unsd = new UnsavedDialog(width*0.5-50, height*0.333);

  Oscillabstract()
  {
    //Add two Elements and a Connection to help users getting started:
    Oscisource source = new Oscisource(10, 100);
    source.index = currentIndex++;
    elements.add(source);
    Oscilloutput output = new Oscilloutput(width-225, height-165);
    output.index = currentIndex++;
    elements.add(output);
    Connection connection = new Connection(0, 0, 1, "Output", "Input");
    connections.add(connection);
  }

  public void resetWorkspace()
  {
    //The output needs to stay here when resetting:
    connections.clear();
    elements.clear();
    currentIndex = 0;
    Oscilloutput output = new Oscilloutput(width-225, height-145);
    output.index = currentIndex++;
    elements.add(output);
    System.gc();
    unsaved = false;
    status.clear();
    status.add("Workspace reset");
  }

  //Each time an Element needs an index, it arrives here
  //The index is used for identification so it's important it's unique
  //So increment each time it's accessed.
  //It doesn't matter if the index of the elements isn't sequential, it should just be unique
  public int getIndex()
  {
    return currentIndex++;
  }

  public void update()
  { 
    //Draw/update the elements:
    for (Oscillelement element : elements)
    {
      element.update();      //Always keep the update() and display() strictly separated to easily implement threading in the future!
      if (element.x > -element.sizex && element.x < width+5 && element.y > -element.sizey && element.y < height+5) element.display(hideElements);
    }

    //Draw/update the connections and remove if indicated:
    for (int i = 0; i < connections.size (); i++)
    {
      Connection connection = connections.get(i);

      connection.update();
      connection.display();
      if (connection.deleteConnection) connections.remove(i);
    }

    //Elements use an alternative delete method: 
    //when indicated they should be deleted they put deleteOscillelement to true
    //and set deletingElementIndex to their own index
    if (deleteOscillelement)
    {
      unsaved = true;
      for (int i = 0; i < elements.size (); i++)
      {
        if (elements.get(i).index == deletingElementIndex)
        {
          elements.remove(i);
          i = elements.size();
        }
      }
      deleteOscillelement = false;
    }

    //Display the new element dialog when necessary:
    if (addNewElement)
    {
      if (theElement != null)
      {
        theElement.update();
        theElement.display();
      }
    }

    //Some GUI magic when resetting workspace (the Clear button):
    if (resetWorkspace)
    {
      if (keyPressed && (key == ENTER || key == RETURN))  //applefags use return instead of enter
      {
        resetWorkspace();
        cp5.getController("emptyOscElements").setLabel("Clear");
        resetWorkspace = false;
      }
      if ((keyPressed && (key != ENTER || key != RETURN)) || mousePressed)
      {
        cp5.getController("emptyOscElements").setLabel("Clear");
        resetWorkspace = false;
      }
    }

    //Drag the workspace around:
    if (dragWorkspace)
    {
      unsaved = true;
      float workspaceX = -mouseX + pmouseX ;
      float workspaceY = -mouseY + pmouseY ;
      for (Oscillelement element : elements)
      {
        element.setPosition(workspaceX, workspaceY);
      }
    }

    //When the mouse is released, stop dragging it around
    if (dragWorkspace && !mousePressed) 
    {
      dragWorkspace = false;
      cursor(ARROW);
    }
  }

  public void mousePressed()
  {
    //Search element and check if it's clicked on:

    if (!keyPressed)
    {
      boolean clickedOnElement = false;
      for (int i = elements.size ()-1; i >= 0; i--)
      {
        Oscillelement element = elements.get(i);
        if (checkMouseOver && mouseX > element.x && mouseX < element.x + element.sizex && mouseY > element.y && mouseY < element.y + element.sizey && !clickedOnElement && !addNewElement)
        {
          unsaved = true;
          clickedOnElement = true;    //used later to check if the new element dialog should be brought up
          element.mouseUpdate();          //tell the element it's been clicked on
          i = -1;
        }
      }
      if (addNewElement)      //the new element dialog is not a part of the elements arraylist so check it independently
      {
        if (mouseX > theElement.x && mouseX < theElement.x + theElement.sizex && mouseY > theElement.y && mouseY < theElement.y + theElement.sizey)
        {
          unsaved = true;
          clickedOnElement = true;
          theElement.mouseUpdate();
        } else
        {
          addNewElement = false;
          showAddDialog = true;
        }
      }


      //Not clicked on anything so bring up the new element dialog:
      if (!clickedOnElement && cp5.getWindow().getMouseOverList().isEmpty() && mouseButton == LEFT)
      {
        if (!addNewElement)
        {
          unsaved = true;
          showAddDialog = !showAddDialog;
          if (showAddDialog) addNewElement = true;
          theElement = new NewElement(mouseX, mouseY);
        }
      }
    }

    //When clicked with the middle mouse button, drag the workspace around:
    if ((mouseButton == CENTER || (mouseButton == LEFT && keyPressed && keyCode == CONTROL))  && !hideElements)
    {
      unsaved = true;
      dragWorkspace = true;
      cursor(MOVE);
    }
  }

  //Convenience method
  public Oscillelement searchElement(int index)
  {
    for (Oscillelement element : elements)
    {
      if (element.index == index) return element;
    }
    return null;
  }

  //Same
  public Oscillelement searchElement(float x, float y)
  {
    for (Oscillelement element : elements)
    {
      if (checkMouseOver && mouseX > element.x && mouseX < element.x + element.sizex && mouseY > element.y && mouseY < element.y + element.sizey)
      {
        return element;
      }
    }
    return null;
  }
}


//Parent class for the elements
class Oscillelement
{
  float x;
  float y;
  float sizex = 100;
  float sizey = 100;
  int index; //index = sequenceCreator.osc.getIndex();
  boolean dragging = false;
  float dragOffsetX = 0;
  float dragOffsetY = 0;
  ArrayList<GuiElement> gui = new ArrayList<GuiElement>();
  String name;
  ArrayList<Node> nodes = new ArrayList<Node>();

  Oscillelement()
  {
  }

  Oscillelement(float x, float y, String name)
  {
    this.x = x;
    this.y = y;
    sizex = 100;
    sizey = 100;
    this.name = name;
    if (sequenceCreator.osc != null)
    {
      index = sequenceCreator.osc.getIndex();
    }
  }

  //This is the adviced constructor
  Oscillelement(float x, float y, float sizex, float sizey, String name)
  {
    this.x = x;
    this.y = y;
    this.sizex = sizex;
    this.sizey = sizey;
    this.name = name;
    if (sequenceCreator.osc != null)
    {
      index = sequenceCreator.osc.getIndex();
    }
  }

  //(Or this one if you're reading in a file)
  Oscillelement(String[] input)
  {
    for (String arg : input)
    {
      String[] q = splitTokens(arg);
      if (q.length >= 2)
      {
        if (q[0].equals("Name") ) name = q[1];
        if (q[0].equals("X") ) x = PApplet.parseInt(q[1]);
        if (q[0].equals("Y") ) y = PApplet.parseInt(q[1]);
        if (q[0].equals("SizeX") ) sizex = PApplet.parseInt(q[1]);
        if (q[0].equals("SizeY") ) sizex = PApplet.parseInt(q[1]);
        if (q[0].equals("Index") ) index = PApplet.parseInt(q[1]);
      }
    }
  }

  //Display this element
  public void display(boolean hide)
  {
    stroke(50);
    strokeWeight(3);
    fill(127);
    float bx = x ;
    float by = y ;
    if (hide) bx += width*10;
    if (hide) by += height*10;

    rect(bx, by, sizex, sizey, 7);

    fill(0);
    textAlign(LEFT);
    textFont(f10);

    text(name, bx+5, by+13);

    for (GuiElement element : gui)
    {
      element.display(bx, by);
    }

    for (Node node : nodes)
    {
      node.display(bx, by);
    }
  }

  //Never display anything in the update() method.
  public void update()
  {
    try
    {
      if (dragging && !mousePressed) dragging = false;
      if (dragging)
      {
        x = mouseX-dragOffsetX;
        y = mouseY - dragOffsetY;
      }

      for (GuiElement element : gui)
      {
        element.update(x, y);
      }

      for (Node node : nodes)
      {
        node.update();
      }
    }
    catch(Exception e)
    {
      println("An error occured while updating element " + index);
      println(e);
      closeElement();
    }
  }

  //This method gets called when the Oscillabstract class detects the user has clicked on this element
  public void mouseUpdate()
  {
    if (!dragging)
    {
      boolean shouldItDrag = true;    //Unless otherwise specified, start dragging
      for (GuiElement element : gui)
      {
        if (element.activateElement(x, y) && element.visible)
        {
          guiActionDetected();
          shouldItDrag = false;    //When clicked on a Gui element, specify otherwise
        }
      }
      for (Node node : nodes)
      {
        if (node.checkMouse(x, y) && node.visible)
        {
          if (mouseButton == LEFT) nodeActionDetected(node);
          if (mouseButton == RIGHT) resetNode(node);
          shouldItDrag = false;
        }
      }
      if (shouldItDrag)
      {

        dragging = true;
        dragOffsetX = mouseX - x;
        dragOffsetY = mouseY - y;
      }
    }
  }

  public void setPosition(float inx, float iny)
  {
    x -= inx;
    y -= iny;
  }

  //When this node has a Connection, the Connection is going to call this method (if it has the right type). 
  //The Connection keeps track of the name of the node as a string, so check which input it corresponds to and apply value
  public void nodeInput(String nodeName, Frame frame)
  {
  }

  //Same here but with floats
  public void nodeInput(String nodeName, float[] input)
  {
  }
  /*
  void nodeInput(String nodeName, float input)
   {
   }
   
   float getFloatValue(String outName)
   {
   return 0;
   }
   */

  //The Connection will call this method when updating. It specifies the name of the node.
  public float[] getFloatArrayValue(String outName)
  {
    float[] empty = {
      0
    };
    return empty;
  }

  //Same but for frames
  public Frame getFrameValue(String outName)
  {
    return null;
  }

  //Handle GUI events here. Loop through all GuiElements and check for their boolean active.
  //If it's true, it has been clicked on.
  public void guiActionDetected()
  {
  }

  //Return a String[] with all the parameters or this element. Be as complete as possible so the exact state can be restored accurately.
  public String[] getElementAsString()
  {
    StringList output = new StringList();
    output.append("Name " + name);
    output.append("X " + x);
    output.append("Y " + y);
    output.append("Index " + index);
    for (GuiElement el : gui)
    {
      output.append("GUI " + el.name + " " + el.getValue());
    }

    return output.array();
  }

  //If clicked on a node, start dragging a connection:
  public void nodeActionDetected(Node node)
  {
    if (node instanceof InNode) 
    {
      //Check if it already has an input (you can't have two connections in the same input): 
      boolean connected = false;
      for (Connection connection : sequenceCreator.osc.connections)
      {
        if (connection.connectedToInput(index, node)) 
        {
          connection.startDraggingInput();
          connected = true;
        }
      }
      if (!connected) 
      {
        Connection connection = new Connection(node.type, node.x+x, node.y+y, node.name, index); //InputNode fixed
        connection.startDraggingOutput();
        sequenceCreator.osc.connections.add(connection);
      }
    }
    if (node instanceof OutNode)
    {
      Connection connection = new Connection(node.type, node.x+x, node.y+y, index, node.name); //OutputNode fixed
      connection.startDraggingInput();
      sequenceCreator.osc.connections.add(connection);
    }
  }

  //Method to reset the value of the node
  public void resetNode(Node node)
  {
  }

  //Allows dynamically resizing the element
  public void reachSize(float newSizex, float newSizey)
  {
    sizex += (newSizex - sizex)*0.25f;
    sizey += (newSizey - sizey)*0.25f;
  }

  //Call this upon exiting the element
  public void closeElement()
  {
    deleteOscillelement = true;
    deletingElementIndex = index;
  }

  //convenience
  public Node getNode(String name)
  {
    for (Node node : nodes)
    {
      if (node.name.equals(name)) return node;
    }
    return null;
  }

  public Node searchNode(float xin, float yin)
  {
    for (Node node : nodes)
    {
      if (xin > x + node.x-5 && xin < x + node.x + 5 && yin > y + node.y-5 && yin < y + node.y +5)
      {
        return node;
      }
    }
    return null;
  }
}

class NewElement extends Oscillelement
{
  int formTime = millis();
  boolean guiVisible = false;

  float yoffset = 0;
  float prevyoffset = 0;
  float blahy = 250;
  float totalshifted = 0;

  NewElement(int x, int y)
  {
    super(x, y, 220, 20, "Add:");
  }

  public void guiActionDetected()
  {
    for (GuiElement element : gui)
    {
      if (element.active) 
      {
        if (element.name.equals("Scroller"))
        {
          element.active = true;
        }
        if (element.name.equals("Source"))
        { 
          element.active = false;
          newSource();
          status.clear();
          status.add("Select which frames are to be used as a source. Sync is the current frame's index.");
        }
        if (element.name.equals("Oscillator"))
        { 
          element.active = false;
          newOscillator();
          status.clear();
          status.add("Click the wave preview window to select a waveform: ");
          status.add("sine, cosine, ramp, triangle, sawtooth, square, random, noise");
        }
        if (element.name.equals("Merger"))
        { 
          element.active = false;
          newMerger();
          status.clear();
          status.add("Merge two or more frames together. ");
          status.add("You can also write specific properties of the first frame into the others.");
        }
        if (element.name.equals("Breakout"))
        { 
          element.active = false;
          newBreakout();
          status.clear();
          status.add("Split a frame into its components (X, Y, Z, R, G, B, ");
          status.add("amount of points, blanked, palette index)");
        }
        if (element.name.equals("Breakin"))
        { 
          element.active = false;
          newBreakin();
          status.clear();
          status.add("Join data together to form a frame.");
        }
        if (element.name.equals("Segment"))
        { 
          element.active = false;
          newSegment();
          status.clear();
          status.add("Outputs a value depending on which segment (unblanked stream of points) the point is.");
        }
        if (element.name.equals("Clip"))
        { 
          element.active = false;
          newClip();
          status.clear();
          status.add("Remove points that are outside the specified box. The box can be moved around with the Anchor values.");
        }
        if (element.name.equals("RGB"))
        { 
          element.active = false;
          newColour();
          status.clear();
          status.add("Assign RGB values to the points of a frame.");
        }
        if (element.name.equals("Palette"))
        { 
          element.active = false;
          newPalette();
          status.clear();
          status.add("Assign a palette index to the points of a frame.");
        }
        if (element.name.equals("Palettifier"))
        { 
          element.active = false;
          newPalettifier();
          status.clear();
          status.add("Make a frame use a palette. RGB values are automatically fitted to the palette colour.");
        }
        if (element.name.equals("Buffershift"))
        {
          element.active = false;
          newBuffershift();
          status.clear();
          status.add("Shift/rotate through a stream of values.");
        }
        if (element.name.equals("RGB 2 HSB"))
        {
          element.active = false;
          newRGB2HSB();
          status.clear();
          status.add("Transform RGB values into Hue/Saturation/Brightness values or vice versa.");
        }
        if (element.name.equals("XYZ 2 R\u03b8\u03c6"))
        {
          element.active = false;
          newXYZ2RThetaPhi();
          status.clear();
          status.add("Transform XYZ coordinates into spheric coordinates.");
        }
        if (element.name.equals("Translate"))
        {
          element.active = false;
          newTranslator();
          status.clear();
          status.add("Translate the points of the frame around.");
        }
        if (element.name.equals("Rotate"))
        {
          element.active = false;
          newRotator();
          status.clear();
          status.add("Rotate the frame around. You can also specify an anchor point around which the frame should rotate.");
        }
        if (element.name.equals("Scale"))
        {
          element.active = false;
          newScaler();
          status.clear();
          status.add("Scale the frame. You can also specify an anchor point to which reference the frame should scale.");
        }
        if (element.name.equals("Constant"))
        {
          element.active = false;
          newConstant();
          status.clear();
          status.add("Drag the mouse to select a value. Use the control key to round to the nearest (half) integer.");
        }
        if (element.name.equals("Add/Subtract"))
        {
          element.active = false;
          newAdder();
          status.clear();
          status.add("Add or subtract two or more values together.");
        }
        if (element.name.equals("Multiply/Divide"))
        {
          element.active = false;
          newMultiplier();
          status.clear();
          status.add("Multiply two or more values.");
        }
        if (element.name.equals("Math"))
        {
          element.active = false;
          newMath();
          status.clear();
          status.add("Transform values with a formula.");
        }
        if (element.name.equals("Logic"))
        {
          element.active = false;
          newLogic();
          status.clear();
          status.add("Boolean operators, if, ...");
        }
        if (element.name.equals("Clock"))
        {
          element.active = false;
          newClock();
          status.clear();
          status.add("Outputs a signal linearly going from 0 to 1 in one second.");
          status.add("The shape of the signal can be altered by connecting a stream of values to Shape.");
        }
        if (element.name.equals("Inspect"))
        {
          element.active = false;
          newInspect();
          status.clear();
          status.add("Hook up some data you want to visualize.");
        }
        if (element.name.equals("Optimise"))
        {
          element.active = false;
          newOptimise();
          status.clear();
          status.add("Reduce point count and optimise frame for scanning.");
        }
      }
    }
    //mousePressed = false;
  }

  public void update()
  {
    super.update();
    blahy = 360;
    if (y > height - blahy) blahy = -y + height;
    reachSize(sizex, blahy);
    if (!guiVisible)
    {
      if (millis() - formTime > blahy)
      {
        gui.add( new GuiScroller(sizex-10, 5, 8, sizey-10, "Scroller"));
        gui.add( new GuiButton(5, 20, 90, 20, "Source"));
        gui.add( new GuiButton(5, 45, 90, 20, "Oscillator"));

        gui.add( new GuiButton(5, 80, 90, 20, "Translate"));
        gui.add( new GuiButton(5, 105, 90, 20, "Rotate"));
        gui.add( new GuiButton(5, 130, 90, 20, "Scale"));

        gui.add( new GuiButton(5, 165, 90, 20, "Merger"));
        gui.add( new GuiButton(5, 190, 90, 20, "Segment"));
        //gui.add( new GuiButton(5, 215, 90, 20, "Clip"));

        gui.add( new GuiButton(5, 250, 90, 20, "RGB"));
        gui.add( new GuiButton(5, 275, 90, 20, "Palette"));
        gui.add( new GuiButton(5, 300, 90, 20, "Palettifier"));

        //gui.add( new GuiButton(5, 330, 90, 20, "Optimise"));

        gui.add( new GuiButton(sizex - 115, 20, 90, 20, "Breakout"));
        gui.add( new GuiButton(sizex - 115, 45, 90, 20, "Breakin"));

        gui.add( new GuiButton(sizex - 115, 80, 90, 20, "Constant"));
        gui.add( new GuiButton(sizex - 115, 105, 90, 20, "Add/Subtract"));
        gui.add( new GuiButton(sizex - 115, 130, 90, 20, "Multiply/Divide"));
        gui.add( new GuiButton(sizex - 115, 155, 90, 20, "Math"));
        gui.add( new GuiButton(sizex - 115, 180, 90, 20, "Logic"));
        gui.add( new GuiButton(sizex - 115, 205, 90, 20, "Clock"));
        gui.add( new GuiButton(sizex - 115, 230, 90, 20, "RGB 2 HSB"));
        //gui.add( new GuiButton(sizex - 115, 255, 90, 20, "XYZ 2 R\u03b8\u03c6"));
        gui.add( new GuiButton(sizex - 115, 280, 90, 20, "Buffershift"));
        gui.add( new GuiButton(sizex - 115, 305, 90, 20, "Inspect"));

        guiVisible = true;
      }
    }
    for (GuiElement el : gui)
    {
      if (el.name.equals("Scroller")) 
      {
        yoffset = el.getValue();
        yoffset = map(yoffset, 0, 1, 0, 360 - blahy);
      } else
      {
        if (yoffset != prevyoffset) 
        {
          el.y  -= yoffset - prevyoffset;
        }
        if (el.y < 0 || el.y > blahy - el.sizey) el.alpha = 0;
        else el.alpha = 255;
      }
    }
    prevyoffset = yoffset;
  }

  public void display()
  {
    super.display(sequenceCreator.osc.hideElements);
  }

  public void newSource()
  {
    sequenceCreator.osc.elements.add(new Oscisource(x, y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }

  public void newOscillator()
  {
    sequenceCreator.osc.elements.add(new Oscillator(x, y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }

  public void newMerger()
  {
    sequenceCreator.osc.elements.add(new Oscimerger((int)x, (int) y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }

  public void newBreakout()
  {
    sequenceCreator.osc.elements.add(new Oscibreakout((int)x, (int) y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  public void newBreakin()
  {
    sequenceCreator.osc.elements.add(new Oscibreakin((int)x, (int) y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  public void newSegment()
  {
    sequenceCreator.osc.elements.add(new Oscisegment((int)x, (int) y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  public void newClip()
  {
    sequenceCreator.osc.elements.add(new Osciclip((int)x, (int) y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  public void newColour()
  {
    sequenceCreator.osc.elements.add(new Oscicolour((int)x, (int) y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  public void newPalette()
  {
    sequenceCreator.osc.elements.add(new Oscipalette((int)x, (int) y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  public void newPalettifier()
  {
    sequenceCreator.osc.elements.add(new Oscipalettifier((int)x, (int) y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  public void newBuffershift()
  {
    sequenceCreator.osc.elements.add(new Oscibuffershift((int)x, (int) y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  public void newTranslator()
  {
    sequenceCreator.osc.elements.add(new Oscitranslate((int) x, (int) y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  public void newRotator()
  {
    sequenceCreator.osc.elements.add(new Oscirotate((int) x, (int) y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  public void newScaler()
  {
    sequenceCreator.osc.elements.add(new Osciscale((int) x, (int) y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  public void newConstant()
  {
    sequenceCreator.osc.elements.add(new Oscilloconstant(x, y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  public void newAdder()
  {
    sequenceCreator.osc.elements.add(new Oscilladder(x, y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  public void newMultiplier()
  {
    sequenceCreator.osc.elements.add(new Osciplier(x, y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  public void newMath()
  {
    sequenceCreator.osc.elements.add(new Oscimath(x, y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  public void newLogic()
  {
    sequenceCreator.osc.elements.add(new Oscilogic(x, y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  public void newClock()
  {
    sequenceCreator.osc.elements.add(new Oscilloclock(x, y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  public void newRGB2HSB()
  {
    sequenceCreator.osc.elements.add(new OsciRGB2HSB(x, y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  public void newXYZ2RThetaPhi()
  {
    sequenceCreator.osc.elements.add(new OsciXYZ2RThetaPhi(x, y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  public void newInspect()
  {
    sequenceCreator.osc.elements.add(new Oscinspect(x, y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  public void newOptimise()
  {
    sequenceCreator.osc.elements.add(new Osciptimize(x, y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
}

class Osciptimize extends Oscillelement
{

  Frame startFrame = new Frame();
  Frame optimisedFrame = new Frame();
  Optimizer opt = new Optimizer();

  Osciptimize(float x, float y)
  {
    super(x, y, 100, 110, "Optimise");
    generateGui();
    generateNodes();
  }

  Osciptimize(String[] input)
  {
    super(input);
    sizex = 100;
    sizey = 110;
    generateGui();
    generateNodes();
    for (String arg : input)
    {
      String[] q = splitTokens(arg);
      if (q.length >= 2)
      {
        if (q[0].equals("File") )
        {


          File selection = sequenceCreator.osc.openedLocation;
          String blah = selection.getPath();
          String[] fixedPath = splitTokens(blah, ".");
          String path = fixedPath[0];
          if (fixedPath.length > 1)
          {
            for (int i = 1; i < fixedPath.length-1; i++)
            {
              path = path + "." + fixedPath[i];
            }
          } 

          path = path + "_optimise_" + index + ".opt";
          try
          {
            File f = new File(path);
            if (f.exists())
            {
              opt.setSettings(loadStrings(path));
            } else
            {
              status.add("Error when trying to read settings file for Optimise element. Make sure this file exists:");
              status.add(path);
            }
          }
          catch(Exception e)
          {
            status.add("Error when trying to read settings file for Optimise element. Make sure this file exists:");
            status.add(path);
          }
        }
      }
    }
  }

  public void update()
  {
    super.update();
    if (startFrame != null) 
    {

      opt.setFrame(startFrame);
      opt.optimise();
      optimisedFrame = opt.getFrame();
    }
  }

  public void display(boolean hide)
  {
    super.display(hide);
    //if (!opt.finished) 
    {

      //opt.display();
      //checkMouseOver = false;
    } 

    if (hide) return;
  }



  public void generateGui()
  {
    //gui.add(new GuiDropdown(5, 80, 90, 20, "Options", true));
    gui.add(new GuiClose(sizex-15, 0));
    gui.add(new GuiButton(sizex-60, 80, 55, 20, "Settings"));
  }

  public void resetNode(Node node)
  {
    if (node.name.equals("Input")) startFrame = new Frame();
  }

  public void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new InNode(10, 25, "Input", 0));
    nodes.add(new OutNode(sizex-10, 25, "Output", 0));
  }

  public void nodeInput(String nodeName, Frame inputFrame)
  {

    if (nodeName.equals("Input"))
    {
      if (inputFrame != null)
      {
        startFrame.points.clear();
        Frame stupidcrapJava = new Frame(inputFrame);

        for (Point point : stupidcrapJava.points)
        {
          startFrame.points.add(new Point(point.clone(point)));
        }
      }
    }
  }

  public Frame getFrameValue(String outName)
  {
    if (outName.equals("Output"))
    {
      return optimisedFrame.clone();
    }
    return null;
  }

  public void guiActionDetected()
  {
    for (GuiElement element : gui)
    {
      if (element.active) 
      {
        if (element.name.equals("Settings"))
        { 
          element.active = false;
          if (!opt.guiVisible)
          {
            OptimizerOverlay oo = new OptimizerOverlay(opt, 50, 50, 480, 320);
            PFrame f = new PFrame(oo, 480, 320);
          }

          //sequenceCreator.osc.hideElements = true;
          //opt.finished = false;
        }

        if (element.name.equals("close"))
        { 
          element.active = false;
          closeElement();
        }
      }
    }
  }



  public String[] getElementAsString()
  {
    String[] superList = super.getElementAsString();
    StringList args = new StringList();


    String name = "_optimise_" + index;
    args.append("File " + name + ".opt");
    File selection = sequenceCreator.osc.openedLocation;
    String blah = selection.getPath();
    String[] fixedPath = splitTokens(blah, ".");
    String path = "";
    if (fixedPath.length > 1)
    {
      for (int i = 0; i < fixedPath.length-1; i++)
      {
        path = path + fixedPath[i];
      }
    } else path = fixedPath[0];
    path = path + name + ".opt";
    saveStrings(path, opt.getSettingsFile());


    return concat(superList, args.array());
  }
}

class OsciRGB2HSB extends Oscillelement
{
  boolean swapped = false;
  float[] inr = {
    0
  };
  float[] ing = {
    0
  };
  float[] inb = {
    0
  };
  float[] outh = {
    0
  };
  float[] outs = {
    0
  };
  float[] outb = {
    0
  };

  OsciRGB2HSB(float x, float y)
  {
    super(x, y, 100, 110, "RGB2HSB");
    generateNodes();
    generateGui();
  }

  OsciRGB2HSB(String[] input)
  {
    super(input);
    sizex = 100;
    sizey = 110;
    generateNodes();
    generateGui();
    for (String arg : input)
    {
      String[] q = splitTokens(arg);
      if (q.length >= 2)
      {
        if (q[0].equals("Swapped"))
        {
          swapped = PApplet.parseBoolean(q[1]);
        }
      }
    }
  }

  public void update()
  {
    super.update();

    int l = max(inr.length, ing.length, inb.length);

    if (l > 0)
    {
      outh = new float[l];
      outs = new float[l];
      outb = new float[l];
      float[] r = mapArray(l, inr, 0);
      float[] g = mapArray(l, ing, 0);
      float[] b = mapArray(l, inb, 0);

      if (swapped)
      {
        for (int i = 0; i < l; i++)
        {
          if (g[i] == 0)  //grey
          {
            outh[i] = b[i];
            outs[i] = b[i];
            outb[i] = b[i];
          } else
          {
            r[i] *= 360;
            int Hi = (int) r[i]/60;
            float f = r[i]/60.0f - Hi;
            float p = b[i]*(1-g[i]);
            float q = b[i]*(1-f*g[i]);
            float t = b[i]*(1-(1-f)*g[i]);

            switch(Hi)
            {
            case 0 :
              outh[i] = b[i];
              outs[i] = t;
              outb[i] = p;
              break;
            case 1 :
              outh[i] = q;
              outs[i] = b[i];
              outb[i] = p;
              break;
            case 2 :
              outh[i] = p;
              outs[i] = b[i];
              outb[i] = t;
              break;
            case 3 :
              outh[i] = p;
              outs[i] = q;
              outb[i] = b[i];
              break;
            case 4 :
              outh[i] = t;
              outs[i] = p;
              outb[i] = b[i];
              break;
            default :
              outh[i] = b[i];
              outs[i] = p;
              outb[i] = q;
              break;
            }
          }
        }
      } else
      {
        for (int i = 0; i < l; i++)
        {
          float max = max(r[i], g[i], b[i]);
          float min = min(r[i], g[i], b[i]);
          if (max == 0)
          {
            outh[i] = 0; 
            outs[i] = 0; 
            outb[i] = 0;
          } else
          {
            if (r[i] >= g[i] && r[i] >= b[i]) outh[i] = ((g[i]-b[i])/(max-min));
            if (g[i] >= r[i] && g[i] >= b[i]) outh[i] = 2+(b[i]-r[i])/((max-min));
            if (b[i] >= r[i] && b[i] >= g[i]) outh[i] = 4+(r[i]-g[i])/((max-min));
            outh[i] = ((60*outh[i])%360)/360f;
            //println(outh[i]);
            outs[i] = (max-min)/max;
            outb[i] = max;
          }
        }
      }
    }
  }

  public void display(boolean hide)
  {
    super.display(hide);
    if (hide) return;
    stroke(0);
    textFont(f10);
    if (swapped)
    {
      text("HSB", x+25, y+35);
      text("RGB", x+sizex-10, y+35);
      text("H", x+23, y+58);
      text("S", x+23, y+78);
      text("B", x+23, y+98);
      text("R", x+sizex-20, y+58);
      text("G", x+sizex-20, y+78);
      text("B", x+sizex-20, y+98);
    } else
    {
      text("RGB", x+25, y+35);
      text("HSB", x+sizex-10, y+35);
      text("R", x+23, y+58);
      text("G", x+23, y+78);
      text("B", x+23, y+98);
      text("H", x+sizex-20, y+58);
      text("S", x+sizex-20, y+78);
      text("B", x+sizex-20, y+98);
    }
  }

  public void generateGui()
  {
    gui.add(new GuiButton(sizex*0.5f-15, 20, 30, 20, "<->"));
    gui.add(new GuiClose(sizex-15, 0));
  }

  public void generateNodes()
  {
    nodes.add(new InNode(10, 55, "_1", 1));
    nodes.add(new InNode(10, 75, "_2", 1));
    nodes.add(new InNode(10, 95, "_3", 1));
    nodes.add(new OutNode(sizex-10, 55, "_4", 1));
    nodes.add(new OutNode(sizex-10, 75, "_5", 1));
    nodes.add(new OutNode(sizex-10, 95, "_6", 1));
  }

  public void resetNode(Node node)
  {
    if (node.name.equals("_1")) inr = new float[] {
      0
    };
    if (node.name.equals("_2")) ing = new float[] {
      0
    };
    if (node.name.equals("_3")) inb = new float[] {
      0
    };
  }

  public void nodeInput(String nodeName, float[] input)
  {

    if (nodeName.equals("_1"))
    {
      inr = input;
    }
    if (nodeName.equals("_2"))
    {
      ing = input;
    }
    if (nodeName.equals("_3"))
    {
      inb = input;
    }
  }

  public float[] getFloatArrayValue(String outName)
  {
    if (outName.equals("_4")) return outh;
    if (outName.equals("_5")) return outs;
    if (outName.equals("_6")) return outb;
    return null;
  }

  public void guiActionDetected()
  {
    for (GuiElement element : gui)
    {
      if (element.active) 
      {
        if (element.name.equals("close"))
        { 
          element.active = false;
          closeElement();
        }
        if (element.name.equals("<->"))
        {
          element.active = false;
          swapped = !swapped;
        }
      }
    }
  }

  public String[] getElementAsString()
  {
    String[] superList = super.getElementAsString();
    StringList args = new StringList();

    args.append("Swapped " + swapped);

    return concat(superList, args.array());
  }
}

class OsciXYZ2RThetaPhi extends Oscillelement
{
  boolean swapped = false;
  boolean origin = false;
  float[] inx = {
    0
  };
  float[] iny = {
    0
  };
  float[] inz = {
    0
  };
  float[] outr = {
    0
  };
  float[] outt = {
    0
  };
  float[] outp = {
    0
  };
  float[] originx = {
    0
  };
  float[] originy = {
    0
  };
  float[] originz = {
    0
  };

  OsciXYZ2RThetaPhi(float x, float y)
  {
    super(x, y, 100, 130, "XYZ2RThetaPhi");
    generateNodes();
    generateGui();
  }

  OsciXYZ2RThetaPhi(String[] input)
  {
    super(input);
    sizex = 100;
    sizey = 130;
    generateNodes();
    generateGui();
    for (String arg : input)
    {
      String[] q = splitTokens(arg);
      if (q.length >= 2)
      {
        if (q[0].equals("Origin") )
        {
          origin = PApplet.parseBoolean(q[1]);
          if (origin)
          {
            for (GuiElement el : gui)
            {
              if (el.name.equals("Origin"))
              {
                el.active = false;
                el.visible = false;
              }
              sizey = 180;

              nodes.add(new InNode(10, 115, "Origin X", 1));
              nodes.add(new InNode(10, 135, "Origin Y", 1));
              nodes.add(new InNode(10, 155, "Origin Z", 1));
            }
          }
        }
        if (q[0].equals("Swapped"))
        {
          swapped = PApplet.parseBoolean(q[1]);
        }
      }
    }
  }

  public void update()
  {
    super.update();

    int l = max(inx.length, iny.length, inz.length);

    if (l > 0)
    {
      outr = new float[l];
      outt = new float[l];
      outp = new float[l];
      float[] x = mapArray(l, inx, 0);
      float[] y = mapArray(l, iny, 0);
      float[] z = mapArray(l, inz, 0);
      float[] ox = mapArray(l, originx, 0);
      float[] oy = mapArray(l, originy, 0);
      float[] oz = mapArray(l, originz, 0);

      if (swapped)  //rtp to xyz
      {
        for (int i = 0; i < l; i++)
        {
          float r = x[i];
          float theta = (y[i]-0.5f)*PI;
          float phi = z[i]*TWO_PI;
          float or = sqrt(sq(ox[i]) + sq(oy[i]) + sq(oz[i]));
          float ot = atan2(ox[i], oy[i]);
          float op = (or == 0) ? 0 : acos(z[i]/or);
          r = r - or;
          theta = theta - ot;
          phi = phi - op;
          outr[i] = r*cos(theta)*cos(phi);
          outt[i] = r*sin(theta);
          outp[i] = r*cos(theta)*sin(phi);
        }
      } else  //xyz to rtp
      {
        for (int i = 0; i < l; i++)
        {
          x[i] = x[i] - ox[i];
          y[i] = y[i] - oy[i];
          z[i] = z[i] - oz[i];
          outr[i] = sqrt(sq(x[i]) + sq(y[i]) + sq(z[i]));
          float b = sqrt( sq(x[i]) + sq(z[i]));
          if (z[i] != 0)
          {
            b*= sign(z[i]);
          } 
          outt[i] = atan2(y[i], b)/TWO_PI;

          outp[i] = atan2(z[i], x[i])/TWO_PI;
        }
      }
    }
  }

  public void display(boolean hide)
  {
    super.display(hide);
    if (hide) return;
    stroke(0);
    textFont(f10);
    textAlign(LEFT);
    if (!swapped)
    {
      text("XYZ", x+5, y+35);
      text("R\u03b8\u03c6", x+sizex-25, y+35);
      text("X", x+20, y+58);
      text("Y", x+20, y+78);
      text("Z", x+20, y+98);
      text("R", x+sizex-25, y+58);
      text("\u03b8", x+sizex-25, y+78);
      text("\u03c6", x+sizex-25, y+98);
    } else
    {
      text("R\u03b8\u03c6", x+5, y+35);
      text("XYZ", x+sizex-25, y+35);
      text("R", x+20, y+58);
      text("\u03b8", x+20, y+78);
      text("\u03c6", x+20, y+98);
      text("X", x+sizex-25, y+58);
      text("Y", x+sizex-25, y+78);
      text("Z", x+sizex-25, y+98);
    }
  }

  public void nodeInput(String nodeName, float[] input)
  {

    if (nodeName.equals("_1"))
    {
      inx = input;
    }
    if (nodeName.equals("_2"))
    {
      iny = input;
    }
    if (nodeName.equals("_3"))
    {
      inz = input;
    }
    if (nodeName.equals("Origin X"))
    {
      originx = input;
    }
    if (nodeName.equals("Origin Y"))
    {
      originy = input;
    }
    if (nodeName.equals("Origin Z"))
    {
      originz = input;
    }
  }

  public void resetNode(Node node)
  {

    if (node.name.equals("_1"))
    {
      inx = new float[1];
      inx[0] = 0;
    }
    if (node.name.equals("_2"))
    {
      iny = new float[1];
      iny[0] = 0;
    }
    if (node.name.equals("_3"))
    {
      inz = new float[1];
      inz[0] = 0;
    }
    if (node.name.equals("Origin X"))
    {
      originx = new float[1];
      originx[0] = 0;
    }
    if (node.name.equals("Origin Y"))
    {
      originy = new float[1];
      originy[0] = 0;
    }
    if (node.name.equals("Origin Z"))
    {
      originz = new float[1];
      originz[0] = 0;
    }
  }

  public float[] getFloatArrayValue(String outName)
  {
    if (outName.equals("_4")) return outr;
    if (outName.equals("_5")) return outt;
    if (outName.equals("_6")) return outp;
    return null;
  }

  public void generateNodes()
  {
    nodes.add(new InNode(10, 55, "_1", 1));
    nodes.add(new InNode(10, 75, "_2", 1));
    nodes.add(new InNode(10, 95, "_3", 1));
    nodes.add(new OutNode(sizex-10, 55, "_4", 1));
    nodes.add(new OutNode(sizex-10, 75, "_5", 1));
    nodes.add(new OutNode(sizex-10, 95, "_6", 1));
  }

  public void generateGui()
  {
    gui.add(new GuiButton(sizex*0.5f-15, 20, 30, 20, "<->"));
    gui.add(new GuiButton(10, sizey-25, 50, 20, "Origin"));
    gui.add(new GuiClose(sizex-15, 0));
  }

  public void guiActionDetected()
  {
    for (GuiElement element : gui)
    {
      if (element.active) 
      {
        if (element.name.equals("close"))
        { 
          element.active = false;
          closeElement();
        }
        if (element.name.equals("<->"))
        {
          element.active = false;
          swapped = !swapped;
        }
        if (element.name.equals("Origin"))
        {
          element.active = false;
          element.visible = false;
          sizey = 180;
          origin = true;

          nodes.add(new InNode(10, 115, "Origin X", 1));
          nodes.add(new InNode(10, 135, "Origin Y", 1));
          nodes.add(new InNode(10, 155, "Origin Z", 1));
        }
      }
    }
  }

  public String[] getElementAsString()
  {
    String[] superList = super.getElementAsString();
    StringList args = new StringList();
    args.append("Swapped " + swapped);
    args.append("Origin " + origin);

    return concat(superList, args.array());
  }
}

class Oscibuffershift extends Oscillelement
{
  float[] values = {
    0
  };
  float[] offset = {
    0
  };
  float[] output = {
    0
  };

  Oscibuffershift(float x, float y)
  {
    super(x, y, 100, 65, "Buffershift");
    generateNodes();
    generateGui();
  }

  Oscibuffershift(String[] input)
  {
    super(input);
    sizex = 100;
    sizey = 65;
    generateNodes();
    generateGui();
  }

  public void update()
  {
    super.update();
    int l = 1;

    if (values.length > 0) 
    {
      l = values.length;
      output = new float[l];
      float[] index = mapArray(l, offset, 0);

      for (int i = 0; i < values.length; i++)
      {
        //output[min((int) (l*(ramp((index[i]+i)/l)*0.5+0.5)), output.length-1)] = values[i];
        int idx = (int)(i+index[i])%l;
        output[i] = values[idx < 0 ? idx +l: idx];
      }
      if (output.length > 0) getNode("Output").setColour(color((int) min(255, max(0, output[0]*255))));
    }
  }

  public void generateGui()
  {
    gui.add(new GuiClose(sizex-15, 0));
  }

  public void generateNodes()
  {
    nodes.add(new InNode(10, 25, "Input", 1));
    nodes.add(new InNode(10, 45, "Offset", 1));
    nodes.add(new OutNode(sizex-10, 25, "Output", 1));
  }

  public void nodeInput(String nodeName, float[] input)
  {

    if (nodeName.equals("Input"))
    {
      values = input;
    }
    if (nodeName.equals("Offset"))
    {
      offset = input;
    }
  }

  public void resetNode(Node node)
  {

    if (node.name.equals("Offset"))
    {
      offset = new float[1];
      offset[0] = 0;
    }
    if (node.name.equals("Input"))
    {
      values = new float[] {
        0
      };
    }
  }

  public float[] getFloatArrayValue(String outName)
  {
    if (outName.equals("Output")) return output;
    return null;
  }

  public void guiActionDetected()
  {
    for (GuiElement element : gui)
    {
      if (element.active) 
      {
        if (element.name.equals("close"))
        { 
          element.active = false;
          closeElement();
        }
      }
    }
  }
  /*
    String[] getElementAsString()
   {
   String[] superList = super.getElementAsString();
   StringList args = new StringList();
   
   
   return concat(superList, args.array());
   }
   */
}

class Oscinspect extends Oscillelement
{
  Frame frame;
  float[] values = {
  };
  float yValue = 60;
  float xValue = 100;
  boolean details = false;
  float minValue, maxValue, cursorY, sum, pminValue, pmaxValue;
  int cursorX;
  boolean overlayVisible = false;

  Oscinspect(float x, float y)
  {
    super(x, y, 100, 160, "Inspect");
    generateGui();
    generateNodes();
  }

  Oscinspect(String[] input)
  {
    super(input);
    sizex = 100;
    sizey = 60;
    generateGui();
    generateNodes();
    for (String arg : input)
    {
      String[] q = splitTokens(arg);
      if (q.length >= 2)
      {
        if (q[0].equals("Details") )
        {
          details = PApplet.parseBoolean(q[1]);
        }
      }
    }
  }

  public void update()
  {
    super.update();
    yValue = 60;
    if (values.length > 0) yValue += 50;
    if (frame != null) yValue += 100;
    reachSize(xValue, yValue);

    if (mouseClicked && frame != null && mouseOver(x+5, y+60, x+95, y+150))
    {
      if (!overlayVisible)
      {
        InspectOverlay io = new InspectOverlay(this, 50, 50, 480, 320);
        PFrame f = new PFrame(io, 480, 320);
      }
    }
  }

  public void display(boolean hide)
  {
    super.display(hide);
    if (hide) return;
    fill(0);
    noStroke();

    int yps = (int) y+60;
    if (values.length > 0)
    {
      if (mouseY >= yps && mouseY <= yps + 40 && mouseX >= (int) x+5 && mouseX <= (int) x+95) cursorX = mouseX-(int) x-5;
      rect(x+5, yps, 90, 40);
      float prev = values[0];
      if (laserboyMode) stroke(color(PApplet.parseInt((sin(PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(PI/2+PApplet.parseFloat(frameCount)/10)*0.5f+0.5f)*255), PApplet.parseInt((sin(3*PI/2+PApplet.parseFloat(frameCount)/20)*0.5f+0.5f)*255))); 
      else stroke(10, 127, 50);
      strokeWeight(1);
      minValue = values[0];
      maxValue = values[0];
      sum = values[0];
      for (int i = 1; i < values.length; i++)
      {
        //Determine minimum:
        if (values[i] < minValue) minValue = values[i];

        //Determine maximum:
        if (values[i] > maxValue) maxValue = values[i];

        //Determine value at cursor position:
        if (i >= cursorX/90f*(values.length-1) && i < (cursorX+1)/90f*(values.length-1)) cursorY = values[i];

        //Calculate sum:
        sum += values[i];

        //Draw:
        line(x+5+(i-1)*(90)/(values.length-1), 20*map(prev, pminValue, pmaxValue, -1, 1)+yps+20, x+5+(i)*(90)/(values.length-1), yps+20+20*map(values[i], pminValue, pmaxValue, -1, 1));
        prev = values[i];
      }
      if (values.length == 1) cursorY = values[0];
      pminValue = minValue;
      pmaxValue = maxValue;
      strokeWeight(1);
      stroke(180);
      line(cursorX+x+5, yps, cursorX+x+5, yps+40);
      yps += 50;
      if (details)
      {
        stroke(0);
        text("Size: " + values.length, x+105, y+20);
        text("X: " + cursorX*values.length/90f, x+105, y+35);
        text("Y: " + cursorY, x+105, y+50);
        text("Min: " + minValue, x+105, y+65);
        text("Max: " + maxValue, x+105, y+95);
        text("Average: " + sum/values.length, x+105, y+80);
      }
    }
    if (frame != null)
    {

      fill(0);
      noStroke();
      rect(x+5, yps, 90, 90);

      frame.drawFrame(x+5, yps, 0, 90, 90, 0, false, true);

      if (details)
      {
        stroke(0);
        if (values.length > 0)
        {
          strokeWeight(1);
          line(x+105, yps-5, x+245, yps-5);
        }
        text("Points: " + frame.points.size(), x+105, yps+15);
        text("Name: " + frame.frameName, x+105, yps+30);
        text("Company: " + frame.companyName, x+105, yps+45);
        text("Palette: " + (frame.palette ? "Yes" : "No"), x+105, yps+60);
        text("Ilda format: " + frame.ildaVersion, x+105, yps+75);
      }
    }
    frame = null;
    values = new float[0];
  }

  public void generateGui()
  {
    gui.add(new GuiClose(sizex-15, 0));
    gui.add(new GuiToggle(sizex-45, 25, 40, 15, "Details"));
  }

  public void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new InNode(10, 25, "Input_fl", 1));
    nodes.add(new InNode(10, 45, "Input_fr", 0));
  }

  public void nodeInput(String nodeName, Frame inputFrame)
  {
    if (nodeName.equals("Input_fr"))
    {
      if (inputFrame != null)
      {
        frame = new Frame(inputFrame);
      }
    }
  }

  public void nodeInput(String nodeName, float[] input)
  {
    if (nodeName.equals("Input_fl")) 
    {
      values = input;
    }
  }

  public void guiActionDetected()
  {
    for (GuiElement element : gui)
    {
      if (element.active) 
      {
        if (element.name.equals("Details"))
        { 
          element.active = false;

          toggleDetails(1-element.getValue());
          element.setValue(1-element.getValue());
        }

        if (element.name.equals("close"))
        { 
          element.active = false;
          closeElement();
        }
      }
    }
  }

  public void toggleDetails(float value)
  {
    if (value == 1)
    {
      details = true;
      xValue = 250;
    } else
    {
      details = false;
      xValue = 100;
    }
  }

  public String[] getElementAsString()
  {
    String[] superList = super.getElementAsString();
    String[] args = new String[1];
    args[0] = "Details " + details;

    return concat(superList, args);
  }
}

class InspectOverlay extends OverlayImage
{
  Oscinspect oi;
  int w, h;
  InspectOverlay(Oscinspect oi, int x, int y, int w, int h)
  {
    super("Inspect", x, y);

    this.oi = oi;
    this.w = w;
    this.h = h;
  }

  public void setup()
  {
    size(w, h, P2D);
    smooth();
  }

  public void draw()
  {
    background(0);
    float s = min(width, height)-10;
    if (oi.frame != null)
    {
      oi.frame.drawFrame(g, 5.0f, 5.0f, 0.0f, s, s, s);
    }
  }
}

class Oscitranslate extends Oscillelement
{
  float[] tx = {    //Three float arrays which store the value by which to translate
    0               //They can have more than one value so each point can be translated over a different amount
  };
  float[] ty = {
    0
  };
  float[] tz = {
    0
  };
  Frame startFrame = new Frame();
  Frame translatedFrame = new Frame();
  float newYValue = 110;

  Oscitranslate(int x, int y)
  {
    super(x, y, 100, 110, "Translate");
    generateGui();
    generateNodes();
  }

  Oscitranslate(String[] input)
  {
    super(input);
    sizex = 100;
    sizey = 110;
    generateGui();
    generateNodes();
  }

  public void update()
  {
    super.update();
    reachSize(sizex, newYValue);
    translatedFrame.points.clear();
    if (startFrame != null) 
    {
      //mapArray(int newSize, float[] input, float defaultvalue) interpolates the array
      float[] ttx = mapArray(startFrame.points.size(), tx, 0);
      float[] tty = mapArray(startFrame.points.size(), ty, 0);
      float[] ttz = mapArray(startFrame.points.size(), tz, 0);

      for (int i = 0; i < startFrame.points.size (); i++)
      {
        Point point = new Point(startFrame.points.get(i));

        //Linearly interpolate between the inputvalues:
        float xind = map(ttx[i], 0, 1, 0, width);
        float yind = map(tty[i], 0, 1, 0, height);
        float zind = map(ttz[i], 0, 1, 0, depth);

        point.position.x += xind; //Translate
        point.position.y += yind;
        point.position.z += zind;
        translatedFrame.points.add(point);
      }
    }
  }

  public void generateGui()
  {
    gui.add(new GuiClose(sizex-15, 0));
  }

  public void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new InNode(10, 25, "Input", 0));
    nodes.add(new InNode(10, 50, "X", 1));
    nodes.add(new InNode(10, 70, "Y", 1));
    nodes.add(new InNode(10, 90, "Z", 1));
    nodes.add(new OutNode(sizex-10, 25, "Output", 0));
  }

  //Gets called by the connection
  public void nodeInput(String nodeName, Frame inputFrame)
  {
    if (nodeName.equals("Input"))
    {
      if (inputFrame != null)
      {
        startFrame.points.clear();
        Frame stupidcrapJava = new Frame(inputFrame);  //clone the frame so the original one doesn't get translated as well
        for (Point point : stupidcrapJava.points)
        {
          startFrame.points.add(new Point(point.clone(point)));
        }
      }
    }
  }

  public void nodeInput(String nodeName, float[] input)
  {
    if (nodeName.equals("X")) tx = input;
    if (nodeName.equals("Y")) ty = input;
    if (nodeName.equals("Z")) tz = input;
  }

  public Frame getFrameValue(String outName)
  {
    if (outName.equals("Output"))
    {
      return translatedFrame.clone();
    }
    return null;
  }

  public void guiActionDetected()
  {
    for (GuiElement element : gui)
    {
      if (element.active) 
      {
        if (element.name.equals("Options"))
        { 
          element.active = false;

          toggleOptions(element.getValue());
          element.setValue(1-element.getValue());
        }

        if (element.name.equals("close"))
        { 
          element.active = false;
          closeElement();
        }
      }
    }
  }

  public void resetNode(Node node)
  {
    if (node.name.equals("X"))
    {
      tx = new float[0];
    }
    if (node.name.equals("Y"))
    {
      ty = new float[0];
    }
    if (node.name.equals("Z"))
    {
      tz = new float[0];
    }
  }

  public void toggleOptions(float displayThem)
  {
    if (displayThem == 1)
    {
      for (GuiElement element : gui)
      {
        if (!element.name.equals("Options") && !element.name.equals("close")) 
        {
          element.visible = true;
        }
      }
      newYValue = 225;
    } else
    {
      for (GuiElement element : gui)
      {
        if (!element.name.equals("Options") && !element.name.equals("close")) 
        {
          element.visible = false;
        }
      }
      newYValue = 110;
    }
  }
}

class Oscirotate extends Oscillelement
{
  float[] rx = {
    0
  };
  float[] ry = {
    0
  };
  float[] rz = {
    0
  };
  //Anchor point(s):
  float[] anx = {
    0
  };
  float[] any = {
    0
  };
  float[] anz = {
    0
  };
  Frame startFrame = new Frame();
  Frame rotatedFrame = new Frame();
  boolean pivot = false;

  Oscirotate(int x, int y)
  {
    super(x, y, 100, 110, "Rotate");
    generateGui();
    generateNodes();
  }

  Oscirotate(String[] input)
  {
    super(input);
    sizex = 100;
    sizey = 110;
    generateGui();
    generateNodes();
    for (String arg : input)
    {
      String[] q = splitTokens(arg);
      if (q.length >= 2)
      {
        if (q[0].equals("Pivot") )
        {
          pivot = PApplet.parseBoolean(q[1]);
          if (pivot)
          {
            for (GuiElement el : gui)
            {
              if (el.name.equals("Pivot point"))
              {
                el.active = false;
                el.visible = false;
              }
              sizey = 170;

              nodes.add(new InNode(10, 110, "Pivot X", 1));
              nodes.add(new InNode(10, 130, "Pivot Y", 1));
              nodes.add(new InNode(10, 150, "Pivot Z", 1));
            }
          }
        }
      }
    }
  }

  public void update()
  {
    super.update();
    rotatedFrame.points.clear();
    if (startFrame != null) 
    {

      //mapArray(int newSize, float[] input, float defaultvalue) interpolates the array
      float[] rrx = mapArray(startFrame.points.size(), rx, 0);    //the angles 
      float[] rry = mapArray(startFrame.points.size(), ry, 0);
      float[] rrz = mapArray(startFrame.points.size(), rz, 0);
      float[] aanx = mapArray(startFrame.points.size(), anx, 0);    //The anchor point
      float[] aany = mapArray(startFrame.points.size(), any, 0);
      float[] aanz = mapArray(startFrame.points.size(), anz, 0);

      float theta, phi, psi;
      float prevtheta = 1; 
      float prevphi = 1;  
      float prevpsi = 1; 
      float[][] R = new float[3][3];    //The rotation matrix

      for (int i = 0; i < startFrame.points.size (); i++)
      {
        Point point = new Point(startFrame.points.get(i)); 

        //Rescale the angles
        theta = rrx[i]*TWO_PI;
        phi = rry[i]*TWO_PI;
        psi = -rrz[i]*TWO_PI;

        //Only calculate the matrix when necessary:
        if (theta != prevtheta || phi != prevphi || psi != prevpsi)
        {
          R = calculateRotationMatrix(theta, phi, psi);
          prevtheta = theta;
          prevphi = phi;
          prevpsi = psi;
        }

        float xind = point.position.x - (aanx[i]+0.5f)*width;
        float yind = point.position.y - (aany[i]+0.5f)*height;
        float zind = point.position.z - (aanz[i]+0.5f)*depth;



        // x' = Rx (matrix multiplication)
        float xnew = R[0][0]*xind + R[0][1]*yind + R[0][2]*zind;
        float ynew = R[1][0]*xind + R[1][1]*yind + R[1][2]*zind;
        float znew = R[2][0]*xind + R[2][1]*yind + R[2][2]*zind;

        //Add the anchor point:
        point.position.x = xnew + (aanx[i]+0.5f)*width;
        point.position.y = ynew + (aany[i]+0.5f)*height;
        point.position.z = znew + (aanz[i]+0.5f)*depth;
        rotatedFrame.points.add(point);
      }
    }
  }



  public void generateGui()
  {
    //gui.add(new GuiDropdown(5, 80, 90, 20, "Options", true));
    gui.add(new GuiClose(sizex-15, 0));
    gui.add(new GuiButton(sizex-60, 80, 55, 20, "Pivot point"));
  }

  public void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new InNode(10, 25, "Input", 0));
    nodes.add(new InNode(10, 50, "X", 1));
    nodes.add(new InNode(10, 70, "Y", 1));
    nodes.add(new InNode(10, 90, "Z", 1));
    nodes.add(new OutNode(sizex-10, 25, "Output", 0));
  }

  public void nodeInput(String nodeName, Frame inputFrame)
  {

    if (nodeName.equals("Input"))
    {
      if (inputFrame != null)
      {
        startFrame.points.clear();
        Frame stupidcrapJava = new Frame(inputFrame);
        for (Point point : stupidcrapJava.points)
        {
          startFrame.points.add(new Point(point.clone(point)));
        }
      }
    }
  }

  public void nodeInput(String nodeName, float[] input)
  {
    if (nodeName.equals("X")) rx = input;
    if (nodeName.equals("Y")) ry = input;
    if (nodeName.equals("Z")) rz = input;
    if (nodeName.equals("Pivot X")) anx = input;
    if (nodeName.equals("Pivot Y")) any = input;
    if (nodeName.equals("Pivot Z")) anz = input;
  }

  public Frame getFrameValue(String outName)
  {
    if (outName.equals("Output"))
    {
      return rotatedFrame.clone();
    }
    return null;
  }

  public void guiActionDetected()
  {
    for (GuiElement element : gui)
    {
      if (element.active) 
      {
        if (element.name.equals("Pivot point"))
        { 
          element.active = false;
          element.visible = false;
          sizey = 170;

          nodes.add(new InNode(10, 110, "Pivot X", 1));
          nodes.add(new InNode(10, 130, "Pivot Y", 1));
          nodes.add(new InNode(10, 150, "Pivot Z", 1));

          pivot = true;
        }

        if (element.name.equals("close"))
        { 
          element.active = false;
          closeElement();
        }
      }
    }
  }

  public void resetNode(Node node)
  {
    if (node.name.equals("X"))
    {
      rx = new float[0];
    }
    if (node.name.equals("Y"))
    {
      ry = new float[0];
    }
    if (node.name.equals("Z"))
    {
      rz = new float[0];
    }
    if (node.name.equals("Pivot X"))
    {
      anx = new float[0];
    }
    if (node.name.equals("Pivot Y"))
    {
      any = new float[0];
    }
    if (node.name.equals("Pivot Z"))
    {
      anz = new float[0];
    }
  }
}

class Osciscale extends Oscillelement
{
  float[] whole = {
    1
  };
  float[] sx = {
    1
  };
  float[] sy = {
    1
  };
  float[] sz = {
    1
  };
  //Anchor point(s):
  float[] anx = {
    0
  };
  float[] any = {
    0
  };
  float[] anz = {
    0
  };
  Frame startFrame = new Frame();
  Frame scaledFrame = new Frame();
  boolean anchor = false;

  Osciscale(int x, int y)
  {
    super(x, y, 100, 130, "Scale");
    generateGui();
    generateNodes();
  }

  Osciscale(String[] input)
  {
    super(input);
    sizex = 100;
    sizey = 130;
    generateGui();
    generateNodes();
    for (String arg : input)
    {
      String[] q = splitTokens(arg);
      if (q.length >= 2)
      {
        if (q[0].equals("Anchor") )
        {
          anchor = PApplet.parseBoolean(q[1]);
          if (anchor)
          {
            for (GuiElement el : gui)
            {
              if (el.name.equals("Anchor point"))
              {
                el.active = false;
                el.visible = false;
              }
              sizey = 190;

              nodes.add(new InNode(10, 130, "Anchor X", 1));
              nodes.add(new InNode(10, 150, "Anchor Y", 1));
              nodes.add(new InNode(10, 170, "Anchor Z", 1));
            }
          }
        }
      }
    }
  }

  public void update()
  {
    super.update();
    scaledFrame.points.clear();
    if (startFrame != null) 
    {
      //mapArray(int newSize, float[] input, float defaultvalue) interpolates the array
      whole = mapArray(startFrame.points.size(), whole, 1);
      float[] ssx = mapArray(startFrame.points.size(), sx, 1);
      float[] ssy = mapArray(startFrame.points.size(), sy, 1);
      float[] ssz = mapArray(startFrame.points.size(), sz, 1);
      float[] aanx = mapArray(startFrame.points.size(), anx, 0);
      float[] aany = mapArray(startFrame.points.size(), any, 0);
      float[] aanz = mapArray(startFrame.points.size(), anz, 0);

      for (int i = 0; i < startFrame.points.size (); i++)
      {
        Point point = new Point(startFrame.points.get(i)); 

        float xind = point.position.x - (aanx[i]+0.5f)*width;
        float yind = point.position.y - (aany[i]+0.5f)*height;
        float zind = point.position.z - (aanz[i]+0.5f)*depth;

        xind = ssx[i]*xind*whole[i];
        yind = ssy[i]*yind*whole[i];
        zind = ssz[i]*zind*whole[i];

        point.position.x = xind + (aanx[i]+0.5f)*width;
        point.position.y = yind + (aany[i]+0.5f)*height;
        point.position.z = zind + (aanz[i]+0.5f)*depth;
        scaledFrame.points.add(point);
      }
    }
  }

  public void generateGui()
  {
    //gui.add(new GuiDropdown(5, 80, 90, 20, "Options", true));
    gui.add(new GuiClose(sizex-15, 0));
    gui.add(new GuiButton(sizex-60, 100, 55, 20, "Anchor point"));
  }

  public void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new InNode(10, 25, "Input", 0));
    nodes.add(new InNode(10, 50, "Combined", 1));
    nodes.add(new InNode(10, 70, "X", 1));
    nodes.add(new InNode(10, 90, "Y", 1));
    nodes.add(new InNode(10, 110, "Z", 1));
    nodes.add(new OutNode(sizex-10, 25, "Output", 0));
  }

  public void nodeInput(String nodeName, Frame inputFrame)
  {

    if (nodeName.equals("Input"))
    {
      if (inputFrame != null)
      {
        startFrame.points.clear();
        Frame stupidcrapJava = new Frame(inputFrame);
        for (Point point : stupidcrapJava.points)
        {
          startFrame.points.add(new Point(point.clone(point)));
        }
      }
    }
  }

  public void nodeInput(String nodeName, float[] input)
  {
    if (nodeName.equals("Combined")) whole = input;
    if (nodeName.equals("X")) sx = input;
    if (nodeName.equals("Y")) sy = input;
    if (nodeName.equals("Z")) sz = input;
    if (nodeName.equals("Anchor X")) anx = input;
    if (nodeName.equals("Anchor Y")) any = input;
    if (nodeName.equals("Anchor Z")) anz = input;
  }

  public Frame getFrameValue(String outName)
  {
    if (outName.equals("Output"))
    {
      return scaledFrame.clone();
    }
    return null;
  }

  public void guiActionDetected()
  {
    for (GuiElement element : gui)
    {
      if (element.active) 
      {
        if (element.name.equals("Anchor point"))
        { 
          element.active = false;
          element.visible = false;
          sizey = 190;

          nodes.add(new InNode(10, 130, "Anchor X", 1));
          nodes.add(new InNode(10, 150, "Anchor Y", 1));
          nodes.add(new InNode(10, 170, "Anchor Z", 1));

          anchor = true;
        }

        if (element.name.equals("close"))
        { 
          element.active = false;
          closeElement();
        }
      }
    }
  }

  public void resetNode(Node node)
  {
    if (node.name.equals("X"))
    {
      sx = new float[0];
    }
    if (node.name.equals("Y"))
    {
      sy = new float[0];
    }
    if (node.name.equals("Z"))
    {
      sz = new float[0];
    }
    if (node.name.equals("Anchor X"))
    {
      anx = new float[0];
    }
    if (node.name.equals("Anchor Y"))
    {
      any = new float[0];
    }
    if (node.name.equals("Anchor Z"))
    {
      anz = new float[0];
    }
    if (node.name.equals("Combined"))
    {
      whole = new float[0];
    }
  }

  public String[] getElementAsString()
  {
    String[] superList = super.getElementAsString();
    StringList args = new StringList();
    args.append("Anchor " + anchor);

    return concat(superList, args.array());
  }
}

class Osciclip extends Oscillelement
{

  float[] x1 = {
    -0.5f
  };
  float[] x2 = {
    0.5f
  };
  float[] y1 = {
    -0.5f
  };
  float[] y2 = {
    0.5f
  };
  float[] z1 = {
    -0.5f
  };
  float[] z2 = {
    0.5f
  };
  //Anchor point(s):
  float[] anx = {
    0
  };
  float[] any = {
    0
  };
  float[] anz = {
    0
  };
  Frame startFrame = new Frame();
  Frame clippedFrame = new Frame();
  boolean anchor = false;

  Osciclip(int x, int y)
  {
    super(x, y, 100, 170, "Clip");
    generateGui();
    generateNodes();
  }

  Osciclip(String[] input)
  {
    super(input);
    sizex = 100;
    sizey = 170;
    generateGui();
    generateNodes();
    for (String arg : input)
    {
      String[] q = splitTokens(arg);
      if (q.length >= 2)
      {
        if (q[0].equals("Anchor") )
        {
          anchor = PApplet.parseBoolean(q[1]);
          if (anchor)
          {
            for (GuiElement el : gui)
            {
              if (el.name.equals("Anchor"))
              {
                el.active = false;
                el.visible = false;
              }
              sizey = 230;

              nodes.add(new InNode(10, 170, "Anchor X", 1));
              nodes.add(new InNode(10, 190, "Anchor Y", 1));
              nodes.add(new InNode(10, 210, "Anchor Z", 1));
            }
          }
        }
      }
    }
  }

  public boolean insideBox(PVector position, float left, float right, float bottom, float top, float front, float back)
  {
    return position.x >= left && position.x <= right && position.y <= bottom && position.y >= top && position.z >= front && position.z <= back;
  }

  public byte getRegion(PVector position, float left, float right, float bottom, float top, float front, float back)
  {
    byte b = 0;
    if (position.x < left) b |= 32;
    if (position.x > right) b |= 16;
    if (position.y > bottom) b |= 8;
    if (position.y < top) b |= 4;
    if (position.z < front) b |= 2;
    if (position.z > back) b |= 1;
    //println(binary(b));
    return b;
  }

  public void update()
  {
    super.update();
    //clippedFrame.points.clear();

    if (startFrame != null && startFrame.points.size() > 0) 
    {
      //ArrayList<Point> pts = startFrame.points;
      //ArrayList<Point> prevpts = new ArrayList<Point>();
      clippedFrame = new Frame(startFrame);
      clippedFrame.points.clear();


      //mapArray(int newSize, float[] input, float defaultvalue) interpolates the array
      int l = startFrame.points.size();
      float[] xx1 = mapArray(l, x1, -0.5f);
      float[] xx2 = mapArray(l, x2, 0.5f);
      float[] yy1 = mapArray(l, y1, -0.5f);
      float[] yy2 = mapArray(l, y2, 0.5f);
      float[] zz1 = mapArray(l, z1, -0.5f);
      float[] zz2 = mapArray(l, z2, 0.5f);
      float[] aanx = mapArray(l, anx, 0);
      float[] aany = mapArray(l, any, 0);
      float[] aanz = mapArray(l, anz, 0);

      Point prevPoint = new Point(startFrame.points.get(startFrame.points.size()-1));
      boolean prevclip = false;

      for (int i = l-1; i >= 0; i--)
      {
        Point point = new Point(startFrame.points.get(i)); 
        int lbound = (int) ((xx1[i]-aanx[i]+0.5f)*width);
        int rbound = (int) ((xx2[i]-aanx[i]+0.5f)*width);
        int tbound = (int) ((yy1[i]-aany[i]+0.5f)*height);
        int bbound = (int) ((yy2[i]-aany[i]+0.5f)*height);
        int fbound = (int) ((zz1[i]-aanz[i]+0.5f)*depth);
        int bkbound = (int) ((zz2[i]-aanz[i]+0.5f)*depth);
        /*
        if (point.position.x >= lbound && prevPoint.position.x >= lbound
         || point.position.x <= rbound && prevPoint.position.x <= rbound
         || point.position.y >= bbound && prevPoint.position.y >= bbound
         || point.position.y <= tbound && prevPoint.position.y <= tbound
         || point.position.z >= fbound && prevPoint.position.z >= fbound
         || point.position.z <= bkbound && prevPoint.position.z <= bkbound)
         {
         */
        //boolean pIB = insideBox(point.position, lbound, rbound, bbound, tbound, fbound, bkbound);
        //boolean prevPIB = insideBox(prevPoint.position, lbound, rbound, bbound, tbound, fbound, bkbound);

        //The pb (point byte) is 0 when the point is inside the clip cube
        //otherwise, the bits determine in which quadrant the point is (first bit: left of lbound, second bit: right of rbound etc)
        byte pb = getRegion(point.position, lbound, rbound, bbound, tbound, fbound, bkbound);
        byte prevpb = getRegion(prevPoint.position, lbound, rbound, bbound, tbound, fbound, bkbound);

        //Add the points when they are inside the cube:
        if (pb == 0 && prevpb == 0)
        {
          clippedFrame.points.add(0, point);
          prevclip = false;
        }

        //When two consecutive points are in a different region, clipping might be necessary:
        //(when they are both in the same region that isn't region 0, they are completely out of bounds and no in-between point should be added)
        if (pb != prevpb)
        {
          Point p = new Point(point);
          float x = point.position.x;
          float y = point.position.y;
          float z = point.position.z;

          if (pb == 0 || prevpb == 0) //One of them is inside the box: add a clipping point in between
          {
            //println("pervpb", binary(prevpb),prevpb, (byte) ((byte) prevpb >> 5),  binary( (((byte) prevpb >> 0x5) & 0xff)), binary(0x1));
            if (prevpb >> 5 == 1 || pb >> 5 == 1)
            {
              //println("clipping", lbound, binary(prevpb), binary(pb), prevpb >> 7, binary(prevpb >> 7));
              x = lbound;
              y = getClipCoord(point.position.x, prevPoint.position.x, point.position.y, prevPoint.position.y, lbound);
              z = getClipCoord(point.position.x, prevPoint.position.x, point.position.z, prevPoint.position.z, lbound);
            }
            if (pb>>4 == 1 || prevpb >> 4 == 1)
            {
              x = rbound;
              y = getClipCoord(point.position.x, prevPoint.position.x, point.position.y, prevPoint.position.y, rbound);
              z = getClipCoord(point.position.x, prevPoint.position.x, point.position.z, prevPoint.position.z, rbound);
            }
            if (pb>>3 == 1 || prevpb >> 3 == 1)
            {
              x = getClipCoord(point.position.y, prevPoint.position.y, point.position.x, prevPoint.position.x, bbound);
              y = bbound;
              z = getClipCoord(point.position.y, prevPoint.position.y, point.position.z, prevPoint.position.z, bbound);
            }

            if (pb>>2 == 1 || prevpb >> 2 == 1)
            {
              x = getClipCoord(point.position.y, prevPoint.position.y, point.position.x, prevPoint.position.x, tbound);
              y = tbound;
              z = getClipCoord(point.position.y, prevPoint.position.y, point.position.z, prevPoint.position.z, tbound);
            }
            if (pb>>1 == 1 || prevpb >> 1 == 1)
            {
              x = getClipCoord(point.position.z, prevPoint.position.y, point.position.x, prevPoint.position.x, fbound);
              y = getClipCoord(point.position.z, prevPoint.position.y, point.position.y, prevPoint.position.y, fbound);
              z = fbound;
            }
            if ((pb & 1) == 1 || (prevpb & 1) == 1)
            {
              x = getClipCoord(point.position.z, prevPoint.position.y, point.position.x, prevPoint.position.x, bkbound);
              y = getClipCoord(point.position.z, prevPoint.position.y, point.position.y, prevPoint.position.y, bkbound);
              z = bkbound;
            }
          } else
          {
            //This gets ugly: the two points are outside of the clipping box but it's possible the line between them is inside
            //(and we want that line to show up)

            //When both points are in the same region (eg both to the left of lbound), don't add them
            boolean add = true;
            for (int j =  0; i < 6; i++)
            {
              if (((pb >> j) & 1) == ((prevpb >> j) & 1)) add = false;
            }
            println(add);
            if (add)
            {
            }
          }

          p.position.set(x, y, z);
          //if (pb != 0) p.blanked = true;  //blank when going back into the bounds

          clippedFrame.points.add(0, p);
          if (pb == 0) clippedFrame.points.add(0, point);
        } 


        prevPoint = point;
      }
    }
  }



  public boolean shouldClip(float in1, float in2, int bound)
  {
    return (in1 < bound && in2 >= bound) || (in1 > bound && in2 <= bound);
  }

  public float getClipCoord(float c1, float c2, float c3, float c4, float clipc)//example: x1, x2, y1, y2, xbound
  {
    return -(c3-c4)*(c1-clipc)/(c1-c2)+c3;
  }

  public void generateGui()
  {
    //gui.add(new GuiDropdown(5, 80, 90, 20, "Options", true));
    gui.add(new GuiClose(sizex-15, 0));
    gui.add(new GuiButton(sizex-50, 140, 45, 20, "Anchor"));
  }

  public void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new InNode(10, 25, "Input", 0));
    nodes.add(new InNode(10, 50, "Left", 1));
    nodes.add(new InNode(10, 70, "Right", 1));
    nodes.add(new InNode(10, 90, "Top", 1));
    nodes.add(new InNode(10, 110, "Bottom", 1));
    nodes.add(new InNode(10, 130, "Back", 1));
    nodes.add(new InNode(10, 150, "Front", 1));
    nodes.add(new OutNode(sizex-10, 25, "Output", 0));
  }

  public void nodeInput(String nodeName, Frame inputFrame)
  {

    if (nodeName.equals("Input"))
    {
      if (inputFrame != null)
      {
        startFrame.points.clear();
        Frame stupidcrapJava = new Frame(inputFrame);
        for (Point point : stupidcrapJava.points)
        {
          startFrame.points.add(new Point(point.clone(point)));
        }
      }
    }
  }

  public void nodeInput(String nodeName, float[] input)
  {
    if (nodeName.equals("Left")) x1 = input;
    if (nodeName.equals("Right")) x2 = input;
    if (nodeName.equals("Top")) y1 = input;
    if (nodeName.equals("Bottom")) y2 = input;
    if (nodeName.equals("Back")) z1 = input;
    if (nodeName.equals("Front")) z2 = input;
    if (nodeName.equals("Anchor X")) anx = input;
    if (nodeName.equals("Anchor Y")) any = input;
    if (nodeName.equals("Anchor Z")) anz = input;
  }

  public Frame getFrameValue(String outName)
  {
    if (outName.equals("Output"))
    {
      return clippedFrame.clone();
    }
    return null;
  }

  public void guiActionDetected()
  {
    for (GuiElement element : gui)
    {
      if (element.active) 
      {
        if (element.name.equals("Anchor"))
        { 
          element.active = false;
          element.visible = false;
          sizey = 230;

          nodes.add(new InNode(10, 170, "Anchor X", 1));
          nodes.add(new InNode(10, 190, "Anchor Y", 1));
          nodes.add(new InNode(10, 210, "Anchor Z", 1));

          anchor = true;
        }

        if (element.name.equals("close"))
        { 
          element.active = false;
          closeElement();
        }
      }
    }
  }

  public void resetNode(Node node)
  {
    if (node.name.equals("Left"))
    {
      x1 = new float[0];
    }
    if (node.name.equals("Right"))
    {
      x2 = new float[0];
    }
    if (node.name.equals("Top"))
    {
      y1 = new float[0];
    }
    if (node.name.equals("Bottom"))
    {
      y2 = new float[0];
    }
    if (node.name.equals("Back"))
    {
      z1 = new float[0];
    }
    if (node.name.equals("Front"))
    {
      z2 = new float[0];
    }

    if (node.name.equals("Anchor X"))
    {
      anx = new float[0];
    }
    if (node.name.equals("Anchor Y"))
    {
      any = new float[0];
    }
    if (node.name.equals("Anchor Z"))
    {
      anz = new float[0];
    }
  }

  public String[] getElementAsString()
  {
    String[] superList = super.getElementAsString();
    StringList args = new StringList();
    args.append("Anchor " + anchor);

    return concat(superList, args.array());
  }
}

class Oscibreakout extends Oscillelement
{

  Frame startFrame = new Frame();

  Oscibreakout(int x, int y)
  {
    super(x, y, 100, 200, "Breakout");
    generateGui();
    generateNodes();
  }

  Oscibreakout(String[] input)
  {
    super(input);
    sizex = 100;
    sizey = 200;
    generateGui();
    generateNodes();
  }

  public void update()
  {
    super.update();
  }

  public void generateGui()
  {
    //gui.add(new GuiDropdown(5, 80, 90, 20, "Options", true));
    gui.add(new GuiClose(sizex-15, 0));
  }

  public void resetNode(Node node)
  {
    if (node.name.equals("Input")) startFrame = new Frame();
  }

  public void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new InNode(10, 25, "Input", 0));
    nodes.add(new OutNode(sizex-10, 25, "X", 1));
    nodes.add(new OutNode(sizex-10, 45, "Y", 1));
    nodes.add(new OutNode(sizex-10, 65, "Z", 1));
    nodes.add(new OutNode(sizex-10, 85, "R", 1));
    nodes.add(new OutNode(sizex-10, 105, "G", 1));
    nodes.add(new OutNode(sizex-10, 125, "B", 1));
    nodes.add(new OutNode(sizex-10, 145, "Total points", 1));
    nodes.add(new OutNode(sizex-10, 165, "Blank", 1));
    nodes.add(new OutNode(sizex-10, 185, "Palette index", 1));
  }

  public void nodeInput(String nodeName, Frame inputFrame)
  {

    if (nodeName.equals("Input"))
    {
      if (inputFrame != null)
      {
        startFrame.points.clear();
        Frame stupidcrapJava = new Frame(inputFrame);
        for (Point point : stupidcrapJava.points)
        {
          startFrame.points.add(new Point(point.clone(point)));
        }
      }
    }
  }

  public float[] getFloatArrayValue(String outName)
  {
    if (outName.equals("X"))
    {
      float[] out = new float[startFrame.points.size()];
      for (int i = 0; i < startFrame.points.size (); i++)
      {
        out[i] = startFrame.points.get(i).position.x/width-0.5f;
      }
      return out;
    }
    if (outName.equals("Y"))
    {
      float[] out = new float[startFrame.points.size()];
      for (int i = 0; i < startFrame.points.size (); i++)
      {
        out[i] = startFrame.points.get(i).position.y/height-0.5f;
      }
      return out;
    }
    if (outName.equals("Z"))
    {
      float[] out = new float[startFrame.points.size()];
      for (int i = 0; i < startFrame.points.size (); i++)
      {
        out[i] = startFrame.points.get(i).position.z/depth-0.5f;
      }
      return out;
    }
    if (outName.equals("R"))
    {
      float[] out = new float[startFrame.points.size()];
      for (int i = 0; i < startFrame.points.size (); i++)
      {
        out[i] = PApplet.parseFloat((startFrame.points.get(i).colour >> 16) & 0xFF)/255;
      }
      return out;
    }
    if (outName.equals("G"))
    {
      float[] out = new float[startFrame.points.size()];
      for (int i = 0; i < startFrame.points.size (); i++)
      {
        out[i] = PApplet.parseFloat((startFrame.points.get(i).colour >> 8) & 0xFF)/255;
      }
      return out;
    }
    if (outName.equals("B"))
    {
      float[] out = new float[startFrame.points.size()];
      for (int i = 0; i < startFrame.points.size (); i++)
      {
        out[i] = PApplet.parseFloat(startFrame.points.get(i).colour & 0xFF)/255;
      }
      return out;
    }
    if (outName.equals("Total points"))
    {
      float[] out = new float[startFrame.points.size()];
      for (int i = 0; i < startFrame.points.size (); i++)
      {
        out[i] = startFrame.points.size();
      }
      return out;
    }
    if (outName.equals("Blank"))
    {
      float[] out = new float[startFrame.points.size()];
      for (int i = 0; i < startFrame.points.size (); i++)
      {
        out[i] = (float) PApplet.parseInt(startFrame.points.get(i).blanked);
      }
      return out;
    }
    if (outName.equals("Palette index"))
    {
      float[] out = new float[startFrame.points.size()];
      for (int i = 0; i < startFrame.points.size (); i++)
      {
        out[i] = (float) startFrame.points.get(i).paletteIndex;
      }
      return out;
    }


    float[] empty = {
      0
    };
    return empty;
  }

  public void guiActionDetected()
  {
    for (GuiElement element : gui)
    {
      if (element.active) 
      {
        if (element.name.equals("close"))
        { 
          element.active = false;
          closeElement();
        }
      }
    }
  }
}

class Oscibreakin extends Oscillelement
{

  Frame outFrame = new Frame();
  int numberOfPoints = 200;
  boolean nOPconnected = false;
  float[] x = {
    0
  };
  float[] y = {
    0
  };
  float[] z = {
    0
  };
  float[] r = {
    0
  };
  float[] g = {
    0
  };
  float[] b = {
    0
  };
  float[] bl = {
    0
  };
  float[] pI = {
    0
  };

  Oscibreakin(int x, int y)
  {
    super(x, y, 100, 200, "Breakin");
    generateGui();
    generateNodes();
  }

  Oscibreakin(String[] input)
  {
    super(input);
    sizex = 100;
    sizey = 200;
    generateGui();
    generateNodes();
    for (String arg : input)
    {
      String[] q = splitTokens(arg);
      if (q.length >= 2)
      {
        if (q[0].equals("NumberOfPoints") )
        {
          numberOfPoints = PApplet.parseInt(q[1]);

          for (int i = 1; i <= numberOfPoints; i++)
          {
            outFrame.points.add(new Point(width*0.5f, height*0.5f, depth*0.5f, 0, 0, 0, true));
          }
        }
      }
    }
  }

  public void update()
  {
    super.update();

    if (!nOPconnected) numberOfPoints = max(x.length, y.length, max(z.length, r.length, max(g.length, b.length, max(bl.length, pI.length))));
    x = mapArray(numberOfPoints, x, 0);
    y = mapArray(numberOfPoints, y, 0);
    z = mapArray(numberOfPoints, z, 0);
    r = mapArray(numberOfPoints, r, 0);
    g = mapArray(numberOfPoints, g, 0);
    b = mapArray(numberOfPoints, b, 0);
    bl = mapArray(numberOfPoints, bl, 0);
    pI = mapArray(numberOfPoints, pI, 0);

    outFrame = new Frame();
    for (int i = 0; i < numberOfPoints; i++)
    {
      Point point = new Point((x[i]+0.5f)*width, (y[i]+0.5f)*height, (z[i]+0.5f)*depth, (int) (r[i]*255), (int) (g[i]*255), (int) (b[i]*255), PApplet.parseBoolean((int) bl[i]));
      point.paletteIndex = (int) pI[i];
      outFrame.points.add(point );
    }
    outFrame.frameName = "Breakin";
    outFrame.companyName = "Oscillabstract";
    outFrame.pointCount = outFrame.points.size();

    nOPconnected = false;
  }

  public void generateGui()
  {
    //gui.add(new GuiDropdown(5, 80, 90, 20, "Options", true));
    gui.add(new GuiClose(sizex-15, 0));
  }

  public void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new OutNode(sizex-10, 25, "Output", 0));
    nodes.add(new InNode(10, 25, "X", 1));
    nodes.add(new InNode(10, 45, "Y", 1));
    nodes.add(new InNode(10, 65, "Z", 1));
    nodes.add(new InNode(10, 85, "R", 1));
    nodes.add(new InNode(10, 105, "G", 1));
    nodes.add(new InNode(10, 125, "B", 1));
    nodes.add(new InNode(10, 145, "Total points", 1));
    nodes.add(new InNode(10, 165, "Blank", 1));
    nodes.add(new InNode(10, 185, "Palette index", 1));
  }

  public void resetNode(Node node)
  {
    if (node.name.equals("X")) x = new float[] {
      0
    };
    if (node.name.equals("Y")) y = new float[] {
      0
    };
    if (node.name.equals("Z")) z = new float[] {
      0
    };
    if (node.name.equals("R")) r = new float[] {
      0
    };
    if (node.name.equals("G")) g = new float[] {
      0
    };
    if (node.name.equals("B")) b = new float[] {
      0
    };
    if (node.name.equals("Total points")) numberOfPoints = 200;
    if (node.name.equals("Blank")) bl = new float[] {
      0
    };
    if (node.name.equals("Palette index")) pI = new float[] {
      0
    };
  }

  public void nodeInput(String nodeName, float[] input)
  {
    //int l = outFrame.points.size();
    if (nodeName.equals("X"))
    {
      x = input;
    }
    if (nodeName.equals("Y"))
    {
      y = input;
    }
    if (nodeName.equals("Z"))
    {
      z = input;
    }
    if (nodeName.equals("R"))
    {
      r = input;
    }
    if (nodeName.equals("G"))
    {
      g = input;
    }
    if (nodeName.equals("B"))
    {
      b = input;
    }
    if (nodeName.equals("Total points"))
    {
      if (input.length > 0) numberOfPoints = (int) input[0];
      nOPconnected = true;
    }
    if (nodeName.equals("Blank"))
    {
      bl = input;
    }
    if (nodeName.equals("Palette index"))
    {
      pI = input;
    }
  }

  public Frame getFrameValue(String outName)
  {
    if (outName.equals("Output")) return outFrame;
    return null;
  }

  public void guiActionDetected()
  {
    for (GuiElement element : gui)
    {
      if (element.active) 
      {
        if (element.name.equals("close"))
        { 
          element.active = false;
          closeElement();
        }
      }
    }
  }
}

class Oscisegment extends Oscillelement
{

  Frame startFrame = new Frame();
  float[] segments = {
    0
  };
  float[] total = {
    0
  };

  Oscisegment(int x, int y)
  {
    super(x, y, 100, 60, "Segment");
    generateGui();
    generateNodes();
  }

  Oscisegment(String[] input)
  {
    super(input);
    sizex = 100;
    sizey = 60;
    generateGui();
    generateNodes();
  }

  public void update()
  {
    super.update();
    int l = startFrame.points.size();
    segments = mapArray(l, segments, 0);
    int t = 0;
    boolean inBetween = false;
    for (int i = 0; i < l; i++)
    {
      Point point = startFrame.points.get(i);
      if (point.blanked && !inBetween)
      {
        inBetween = true;
        t++;
        segments[i] = t;
      } else
      {
        segments[i] = t;
        inBetween = false;
      }
    }
    float v = 1f/t;
    for (int i = 0; i < l; i++)
    {
      segments[i] *= v;
    }
    total[0] = t;
  }

  public void generateGui()
  {
    //gui.add(new GuiDropdown(5, 80, 90, 20, "Options", true));
    gui.add(new GuiClose(sizex-15, 0));
  }

  public void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new InNode(10, 25, "Input", 0));
    nodes.add(new OutNode(sizex-10, 45, "Segments", 1));
    nodes.add(new OutNode(sizex-10, 25, "Total", 1));
  }

  public void resetNode(Node node)
  {
    if (node.name.equals("Input")) startFrame = new Frame();
  }

  public void nodeInput(String nodeName, Frame inputFrame)
  {

    if (nodeName.equals("Input"))
    {
      if (inputFrame != null)
      {
        startFrame.points.clear();
        Frame stupidcrapJava = new Frame(inputFrame);
        for (Point point : stupidcrapJava.points)
        {
          startFrame.points.add(new Point(point.clone(point)));
        }
      }
    }
  }

  public float[] getFloatArrayValue(String outName)
  {
    if (outName.equals("Segments"))
    {
      return segments;
    }
    if (outName.equals("Total"))
    {
      return total;
    }

    float[] empty = {
      0
    };
    return empty;
  }

  public void guiActionDetected()
  {
    for (GuiElement element : gui)
    {
      if (element.active) 
      {
        if (element.name.equals("close"))
        { 
          element.active = false;
          closeElement();
        }
      }
    }
  }
}

class Oscicolour extends Oscillelement
{

  Frame outFrame = new Frame();
  Frame inFrame = new Frame();
  boolean multiply = false;
  float[] r = {
    1
  };
  float[] g = {
    1
  };
  float[] b = {
    1
  };
  float[] ity = {
    1
  };

  Oscicolour(int x, int y)
  {
    super(x, y, 100, 120, "RGB");
    generateGui();
    generateNodes();
  }

  Oscicolour(String[] input)
  {
    super(input);
    sizex = 100;
    sizey = 120;
    generateGui();
    generateNodes();
    for (String arg : input)
    {
      String[] q = splitTokens(arg);
      if (q.length >= 2)
      {
        if (q[0].equals("Multiply") )
        {
          multiply = PApplet.parseBoolean(q[1]);
        }
      }
    }
  }

  public void update()
  {
    super.update();
    int l = inFrame.points.size();
    if (multiply)
    {
      r = mapArray(l, r, 1);
      g = mapArray(l, g, 1);
      b = mapArray(l, b, 1);
    } else
    {
      r = mapArray(l, r, 0);
      g = mapArray(l, g, 0);
      b = mapArray(l, b, 0);
    }
    ity = mapArray(l, ity, 1);
    outFrame = new Frame(inFrame.clone());
    for (int i = 0; i < l; i++)
    {
      if (multiply)
      {
        int nlg = outFrame.points.get(i).colour;
        nlg = color(   max(0, min(((nlg >> 16) & 0xFF) * r[i]*ity[i], 255)), max(0, min(((nlg >> 8) & 0xFF) * g[i]*ity[i], 255)), max(0, min(((nlg  & 0xFF) * b[i]*ity[i]), 255)));
        outFrame.points.get(i).colour = nlg;
      } else
      {
        outFrame.points.get(i).colour = color(max(0, min((r[i]*ity[i])*255, 255)), max(0, min((g[i]*ity[i])*255, 255)), max(0, min((b[i]*ity[i])*255, 255)));
      }
    }
  }

  public void generateGui()
  {
    gui.add(new GuiButton(sizex - 55, 70, 50, 20, "Overwrite"));
    gui.add(new GuiClose(sizex-15, 0));
  }

  public void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new OutNode(sizex-10, 25, "Output", 0));
    nodes.add(new InNode(10, 25, "Input", 0));
    nodes.add(new InNode(10, 45, "R", 1));
    nodes.add(new InNode(10, 65, "G", 1));
    nodes.add(new InNode(10, 85, "B", 1));
    nodes.add(new InNode(10, 105, "I", 1));
  }

  public void nodeInput(String nodeName, Frame input)
  {
    if (nodeName.equals("Input")) 
    {
      if (input != null) inFrame = new Frame(input.clone());
    }
  }

  public void nodeInput(String nodeName, float[] input)
  {

    if (nodeName.equals("R"))
    {
      if (input != null) r = input;
    }
    if (nodeName.equals("G"))
    {
      if (input != null) g = input;
    }
    if (nodeName.equals("B"))
    {
      if (input != null) b = input;
    }
    if (nodeName.equals("I"))
    {
      if (input != null) ity = input;
    }
  }

  public void resetNode(Node node)
  {
    if (node.name.equals("R"))
    {
      r = new float[0];
    }
    if (node.name.equals("G"))
    {
      g = new float[0];
    }
    if (node.name.equals("B"))
    {
      b = new float[0];
    }
    if (node.name.equals("I"))
    {
      ity = new float[0];
    }
  }

  public Frame getFrameValue(String outName)
  {
    if (outName.equals("Output")) return outFrame;
    return null;
  }

  public void guiActionDetected()
  {
    for (GuiElement element : gui)
    {
      if (element.active) 
      {
        if (element.name.equals("close"))
        { 
          element.active = false;
          closeElement();
        }
        if (element.name.equals("Overwrite"))
        { 
          element.active = false;
          if (multiply)
          {
            multiply = false;
            element.text = "Overwrite";
          } else
          {
            multiply = true;
            element.text = "Multiply";
          }
          r = new float[0];
          g = new float[0];
          b = new float[0];
          ity = new float[0];
        }
      }
    }
  }

  public String[] getElementAsString()
  {
    String[] superList = super.getElementAsString();
    StringList args = new StringList();
    args.append("Multiply " + multiply);

    return concat(superList, args.array());
  }
}

class Oscipalette extends Oscillelement
{
  Frame outFrame = new Frame();
  Frame inFrame = new Frame();
  Palette palette;
  float newYValue = 80;
  boolean options = false;
  boolean connected = false;
  int palind = activePalette;

  float[] ind = {
    0
  };


  Oscipalette(int x, int y)
  {
    super(x, y, 100, 80, "Palette");
    generateGui();
    generateNodes();
    if (activePalette < palettes.size() && activePalette >= 0) palette = palettes.get(activePalette);
    for (GuiElement element : gui)
    {
      if (!element.name.equals("Options") && !element.name.equals("close")) 
      {
        element.visible = false;
      }
    }
  }

  Oscipalette(String[] input)
  {
    super(input);
    sizex = 100;
    sizey = 80;
    generateGui();
    generateNodes();
    for (String arg : input)
    {
      String[] q = splitTokens(arg);
      if (q.length >= 2)
      {
        if (q[0].equals("GUI") )
        {
          if (q[1].equals("Options"))
          { 
            toggleOptions(PApplet.parseFloat(q[2]));
            if (PApplet.parseFloat(q[2]) == 0) options = false;
          }
        }
        if (q[0].equals("Palettefile"))
        {
          String paletteName = q[1];
          if (q.length > 2)
          {
            for (int i = 2; i < q.length; i++)
            {
              paletteName = paletteName + " " + q[i];
            }
          }

          File selection = sequenceCreator.osc.openedLocation;
          String blah = selection.getPath();
          String[] fixedPath = splitTokens(blah, ".");
          String path = "";
          if (fixedPath.length > 1)
          {
            for (int i = 0; i < fixedPath.length-1; i++)
            {
              path = path + fixedPath[i];
            }
          } else path = fixedPath[0];

          path = path + paletteName + ".png";

          PImage img;
          try {
            img = loadImage(path);
          }
          catch(Exception e)
          {
            status.add("Error when trying to load in palette");
            return;
          }

          palette = new Palette(paletteName);

          if (img != null)
          {
            for ( int i = 0; i < min (img.width, 256); i++)
            {
              palette.addColour(img.get(i, 0));
            }
          } else
          {
            status.add("Error: empty palette loaded.");
          }
        }
      }
    }

    if (palette == null && activePalette < palettes.size() && activePalette >= 0) palette = palettes.get(activePalette);

    if (!options)
    {
      for (GuiElement element : gui)
      {
        if (!element.name.equals("Options") && !element.name.equals("close")) 
        {
          element.visible = false;
        }
      }
    }
  }

  public void update()
  {
    super.update();
    reachSize(sizex, newYValue);
    int l = inFrame.points.size();   
    ind = mapArray(l, ind, 0);
    outFrame = new Frame(inFrame.clone());
    outFrame.palette = true;
    if (connected)
    {
      for (int i = 0; i < l; i++)
      {

        outFrame.points.get(i).paletteIndex = (int) ind[i];
      }
    }
    outFrame.palettePaint(palette);
    connected = false;
  }

  public void display(boolean hide)
  {
    super.display(hide);
    if (hide) return;

    if (options)
    {
      int i = 0;
      for (PaletteColour colour : palette.colours)
      {
        noStroke();

        colour.displayColour((int) x+10+5*(i%16), (int) y+110+5*PApplet.parseInt(i/16), 5);
        i++;
      }
      fill(0);
      textAlign(LEFT);
      textFont(f10);
      text(palette.name, x+10, y+127+5*(palette.colours.size()/16));
    }
  }

  public void generateGui()
  {
    gui.add(new GuiDropdown(5, 50, 90, 20, "Options", true));
    gui.add(new GuiButton(5, 80, 40, 20, "<<"));
    gui.add(new GuiButton(55, 80, 40, 20, ">>"));
    gui.add(new GuiClose(sizex-15, 0));
  }

  public void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new OutNode(sizex-10, 25, "Output", 0));
    nodes.add(new InNode(10, 25, "Input", 0));
    nodes.add(new InNode(10, 45, "Index", 1));
  }


  public void nodeInput(String nodeName, Frame input)
  {
    if (nodeName.equals("Input")) 
    {
      if (input != null) inFrame = new Frame(input.clone());
    }
  }

  public void nodeInput(String nodeName, float[] input)
  {

    if (nodeName.equals("Index"))
    {
      connected = true;
      if (input != null) ind = input;
    }
  }

  public void resetNode(Node node)
  {

    if (node.name.equals("Index"))
    {
      ind = new float[0];
    }
    if (node.name.equals("Input")) inFrame = new Frame();
  }

  public Frame getFrameValue(String outName)
  {
    if (outName.equals("Output")) return outFrame;
    return null;
  }

  public void guiActionDetected()
  {
    for (GuiElement element : gui)
    {
      if (element.active) 
      {
        if (element.name.equals("close"))
        { 
          element.active = false;
          closeElement();
        }
        if (element.name.equals("Options"))
        {
          element.active  = false;
          toggleOptions(element.getValue());
          element.setValue(1-element.getValue());
        }
        if (element.name.equals("<<"))
        {
          element.active  = false;
          palind--;
          if (palind < 0) palind = palettes.size()-1;
          palette = palettes.get(palind);
          newYValue = 135 + 5*(palette.colours.size()/16);
        }
        if (element.name.equals(">>"))
        {
          element.active  = false;
          palind++;
          if (palind >= palettes.size() ) palind = 0;
          palette = palettes.get(palind);
          newYValue = 135 + 5*(palette.colours.size()/16);
        }
      }
    }
  }

  public void toggleOptions(float displayThem)
  {
    if (displayThem == 1)
    {
      for (GuiElement element : gui)
      {
        if (!element.name.equals("Options") && !element.name.equals("close")) 
        {
          element.visible = true;
        }
      }
      if (palette != null) newYValue = 135 + 5*((int) palette.colours.size()/16);
      options = true;
    } else
    {
      for (GuiElement element : gui)
      {
        if (!element.name.equals("Options") && !element.name.equals("close")) 
        {
          element.visible = false;
        }
      }
      newYValue = 80;
      options = false;
    }
  }

  public String[] getElementAsString()
  {
    String[] superList = super.getElementAsString();
    StringList args = new StringList();
    if (palette != null)
    {
      File selection = sequenceCreator.osc.openedLocation;
      String blah = selection.getPath();
      String[] fixedPath = splitTokens(blah, ".");
      String path = "";
      if (fixedPath.length > 1)
      {
        for (int i = 0; i < fixedPath.length-1; i++)
        {
          path = path + fixedPath[i];
        }
      } else path = fixedPath[0];
      path = path + palette.name + ".png";

      args.append("Palettefile " + palette.name);

      PImage pg = createImage(palette.colours.size(), 1, RGB);
      for (int i = 0; i < palette.colours.size (); i++)
      {
        pg.pixels[i] = palette.colours.get(i).getColour();
      }
      pg.save(path);
      println("Saved palette!", palette.name, sequenceCreator.osc.openedLocation);
    }
    return concat(superList, args.array());
  }
}

class Oscipalettifier extends Oscillelement
{
  Frame outFrame = new Frame();
  Frame inFrame = new Frame();
  Palette palette;
  float newYValue = 80;
  boolean options = false;
  int palind = activePalette;

  float[] ind = {
    0
  };


  Oscipalettifier(int x, int y)
  {
    super(x, y, 100, 80, "Palettifier");
    generateGui();
    generateNodes();
    if (activePalette < palettes.size() && activePalette >= 0) palette = palettes.get(activePalette);
    for (GuiElement element : gui)
    {
      if (!element.name.equals("Options") && !element.name.equals("close")) 
      {
        element.visible = false;
      }
    }
  }

  Oscipalettifier(String[] input)
  {
    super(input);
    sizex = 100;
    sizey = 80;
    generateGui();
    generateNodes();
    for (String arg : input)
    {
      String[] q = splitTokens(arg);
      if (q.length >= 2)
      {
        if (q[0].equals("GUI") )
        {
          if (q[1].equals("Options"))
          { 
            toggleOptions(PApplet.parseFloat(q[2]));
            if (PApplet.parseFloat(q[2]) == 0) options = false;
          }
        }
        if (q[0].equals("Palettefile"))
        {
          String paletteName = q[1];
          if (q.length > 2)
          {
            for (int i = 2; i < q.length; i++)
            {
              paletteName = paletteName + " " + q[i];
            }
          }

          File selection = sequenceCreator.osc.openedLocation;
          String blah = selection.getPath();
          String[] fixedPath = splitTokens(blah, ".");
          String path = "";
          if (fixedPath.length > 1)
          {
            for (int i = 0; i < fixedPath.length-1; i++)
            {
              path = path + fixedPath[i];
            }
          } else path = fixedPath[0];

          path = path + paletteName + ".png";
          PImage img;
          try {
            img = loadImage(path);
          }
          catch(Exception e)
          {
            status.add("Error when trying to load in palette");
            return;
          }

          palette = new Palette(paletteName);

          if (img != null)
          {

            for ( int i = 0; i < min (img.width, 256); i++)
            {
              palette.addColour(img.get(i, 0));
            }
          } else
          {
            status.add("Error: empty palette loaded");
          }
        }
      }
    }

    if (palette == null && activePalette < palettes.size() && activePalette >= 0) palette = palettes.get(activePalette);

    if (!options)
    {
      for (GuiElement element : gui)
      {
        if (!element.name.equals("Options") && !element.name.equals("close")) 
        {
          element.visible = false;
        }
      }
    }
  }

  public void update()
  {
    super.update();
    reachSize(sizex, newYValue);
    int l = inFrame.points.size();   
    ind = mapArray(l, ind, 0);
    outFrame = new Frame(inFrame.clone());
    outFrame.palette = true;

    for (int i = 0; i < l; i++)
    {
      ind[i] = outFrame.points.get(i).getBestFittingPaletteColourIndex(palette);
      outFrame.points.get(i).paletteIndex = (int) ind[i];
    }

    //outFrame.palettePaint(palette);
  }

  public void display(boolean hide)
  {
    super.display(hide);
    if (hide) return;

    if (options)
    {
      int i = 0;
      for (PaletteColour colour : palette.colours)
      {
        noStroke();

        colour.displayColour((int) x+10+5*(i%16), (int) y+110+5*PApplet.parseInt(i/16), 5);
        i++;
      }
      fill(0);
      textAlign(LEFT);
      textFont(f10);
      text(palette.name, x+10, y+127+5*(palette.colours.size()/16));
    }
  }

  public void generateGui()
  {
    gui.add(new GuiDropdown(5, 50, 90, 20, "Options", true));
    gui.add(new GuiButton(5, 80, 40, 20, "<<"));
    gui.add(new GuiButton(55, 80, 40, 20, ">>"));
    gui.add(new GuiClose(sizex-15, 0));
  }

  public void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new OutNode(sizex-10, 25, "Output", 0));
    nodes.add(new InNode(10, 25, "Input", 0));
    nodes.add(new OutNode(sizex-10, 45, "Index", 1));
  }

  public void nodeInput(String nodeName, Frame input)
  {
    if (nodeName.equals("Input")) 
    {
      if (input != null) inFrame = new Frame(input.clone());
    }
  }

  public void resetNode(Node node)
  {
    if (node.name.equals("Input")) inFrame = new Frame();
  }

  public Frame getFrameValue(String outName)
  {
    if (outName.equals("Output")) return outFrame;
    return null;
  }

  public float[] getFloatArrayValue(String name)
  {
    if (name.equals("Index")) return ind;
    return null;
  }

  public void guiActionDetected()
  {
    for (GuiElement element : gui)
    {
      if (element.active) 
      {
        if (element.name.equals("close"))
        { 
          element.active = false;
          closeElement();
        }
        if (element.name.equals("Options"))
        {
          element.active  = false;
          toggleOptions(element.getValue());
          element.setValue(1-element.getValue());
        }
        if (element.name.equals("<<"))
        {
          element.active  = false;
          palind--;
          if (palind < 0) palind = palettes.size()-1;
          palette = palettes.get(palind);
          newYValue = 135 + 5*(palette.colours.size()/16);
        }
        if (element.name.equals(">>"))
        {
          element.active  = false;
          palind++;
          if (palind >= palettes.size() ) palind = 0;
          palette = palettes.get(palind);
          newYValue = 135 + 5*(palette.colours.size()/16);
        }
      }
    }
  }

  public void toggleOptions(float displayThem)
  {
    if (displayThem == 1)
    {
      for (GuiElement element : gui)
      {
        if (!element.name.equals("Options") && !element.name.equals("close")) 
        {
          element.visible = true;
        }
      }
      newYValue = 135 + 5*((int) palette.colours.size()/16);
      options = true;
    } else
    {
      for (GuiElement element : gui)
      {
        if (!element.name.equals("Options") && !element.name.equals("close")) 
        {
          element.visible = false;
        }
      }
      newYValue = 80;
      options = false;
    }
  }

  public String[] getElementAsString()
  {
    String[] superList = super.getElementAsString();
    StringList args = new StringList();
    if (palette != null)
    {
      File selection = sequenceCreator.osc.openedLocation;
      String blah = selection.getPath();
      String[] fixedPath = splitTokens(blah, ".");
      String path = "";
      if (fixedPath.length > 1)
      {
        for (int i = 0; i < fixedPath.length-1; i++)
        {
          path = path + fixedPath[i];
        }
      } else path = fixedPath[0];
      path = path + palette.name + ".png";

      args.append("Palettefile " + palette.name);

      PImage pg = createImage(palette.colours.size(), 1, RGB);
      for (int i = 0; i < palette.colours.size (); i++)
      {
        pg.pixels[i] = palette.colours.get(i).getColour();
      }
      pg.save(path);
    }
    return concat(superList, args.array());
  }
}

class Oscimerger extends Oscillelement
{
  boolean sendX, sendY, sendZ, sendR, sendG, sendB, sendBl, sendPalInd, sendNr;
  float newYValue = 80;
  Frame firstFrame = new Frame();
  ArrayList<Frame> secondFrames = new ArrayList<Frame>();
  Frame mergedFrame = new Frame();
  int numberOfInputs;
  float[] mergOptions = new float[10];
  boolean extended = false;


  Oscimerger(int x, int y)
  {
    super(x, y, 120, 80, "Merge");

    generateGui();
    generateNodes();
  }

  Oscimerger(String[] input)
  {
    super(input);
    sizex = 120;
    sizey = 80;
    generateGui();
    generateNodes();
    for (String arg : input)
    {
      String[] q = splitTokens(arg);
      if (q.length >= 2)
      {
        if (q[0].equals("NumberOfInputs") )
        {
          numberOfInputs = PApplet.parseInt(q[1]);

          for (int i = 1; i <= numberOfInputs; i++)
          {
            nodes.add(new InNode(10, 45+(i)*20, "Frame " + (i +2), 0));

            for (GuiElement element : gui)
            {
              if (!element.name.equals("close")) element.y += 20;
            }
            newYValue+=20;
          }
        }
        if (q[0].equals("GUI") )
        {
          if (q[1].equals("Options"))
          { 
            toggleOptions(PApplet.parseFloat(q[2]));
          }
          if (q[1].equals("close"))
          {
          }
          if (!q[1].equals("Options") && !q[1].equals("close") && !q[1].equals("Add") && !q[1].equals("None"))
          { 
            String elName = q[1];

            for (GuiElement el : gui)
            {
              if (el.name.equals(elName))
              {
                el.active = true;
                el.setValue(1-PApplet.parseInt(q[2]));
                guiActionDetected();
              }
            }
          }
        }
      }
    }
  }

  public void generateGui()
  {
    gui.add(new GuiDropdown(5, 55, 110, 20, "Options", true));
    gui.add(new GuiButton(5, 75, 110, 20, "Add node"));
    gui.add(new GuiToggle(5, 160, 15, 15, "X"));
    gui.add(new GuiToggle(23, 160, 15, 15, "Y"));
    gui.add(new GuiToggle(41, 160, 15, 15, "Z"));
    gui.add(new GuiToggle(60, 160, 15, 15, "R"));
    gui.add(new GuiToggle(78, 160, 15, 15, "G"));
    gui.add(new GuiToggle(96, 160, 15, 15, "B"));
    gui.add(new GuiToggle(5, 180, 50, 15, "Number"));
    gui.add(new GuiToggle(60, 180, 50, 15, "Blanking"));
    gui.add(new GuiToggle(5, 200, 50, 15, "Palette"));
    gui.add(new GuiButton(60, 200, 50, 15, "None"));
    gui.add(new GuiClose(sizex-15, 0));

    for (GuiElement element : gui)
    {
      if (!element.name.equals("Options") && !element.name.equals("close")) element.visible = false;
    }
  }

  public void update()
  {
    super.update();
    reachSize(sizex, newYValue);
    mergedFrame.points.clear();
    boolean mergMode = false;
    for (int i = 0; i < mergOptions.length; i++)
    {
      if (mergOptions[i] == 1) mergMode = true;
    }
    //println(mergMode + " " + secondFrames.isEmpty());

    if (firstFrame != null && !mergMode) 
    {
      mergedFrame.points.addAll(firstFrame.points);
    }
    if (!secondFrames.isEmpty()) 
    {
      for (Frame frame : secondFrames)
      {
        if (!frame.points.isEmpty()) 
        {
          if (mergMode)
          {
            frame.merge(firstFrame, mergOptions);
          } else
          {
            Point firstPoint = frame.points.get(0);
            mergedFrame.points.add(new Point(firstPoint.position, (firstPoint.colour >> 16) & 0xFF, (firstPoint.colour >> 8) & 0xFF, firstPoint.colour & 0xFF, true));
          }
        }
        mergedFrame.points.addAll(frame.points);
      }
    }
    firstFrame.points.clear();
    secondFrames = new ArrayList<Frame>(numberOfInputs+1);
    for (int i = 0; i <= numberOfInputs; i++)
    {
      secondFrames.add(new Frame());
    }
  }

  public void display(boolean hide)
  {
    if (hide) return;
    super.display(hide);
    if (extended)
    {
      fill(0);
      textFont(f10);
      textAlign(LEFT);
      text("Write these properties of Frame 1 to the other frames:", x+ 5, y+100+20*numberOfInputs, 110, 60);
    }
  }

  public void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new InNode(10, 25, "Frame 1", 0));
    nodes.add(new InNode(10, 45, "Frame 2", 0));
    nodes.add(new OutNode(sizex-10, 25, "Output", 0));
  }

  public void nodeInput(String nodeName, Frame inputFrame)
  {

    if (nodeName.equals("Frame 1"))
    {
      if (inputFrame != null)
      {
        firstFrame = new Frame(inputFrame.clone());
      }
    }

    for (int i = 0; i <= numberOfInputs; i++)
    {
      if (nodeName.equals("Frame " + (i+2)))
      {
        if (inputFrame != null)
        {
          //println("i " + i + "frame " + (i+2) + "nrinputs " + numberOfInputs);
          secondFrames.set(i, new Frame(inputFrame.clone()));
        }
      }
    }
  }

  public Frame getFrameValue(String outName)
  {
    if (outName.equals("Output"))
    {
      return mergedFrame;
    }
    return null;
  }

  public void guiActionDetected()
  {
    for (GuiElement element : gui)
    {
      if (element.active) 
      {
        if (element.name.equals("Options"))
        { 
          element.active = false;

          toggleOptions(element.getValue());
          element.setValue(1-element.getValue());
        }

        if (element.name.equals("Add node"))
        { 
          element.active = false;

          addInputNode();
        }

        if (element.name.equals("X"))
        { 
          element.active = false;
          element.toggle();
          actX(element.getValue());
        }

        if (element.name.equals("Y"))
        { 
          element.active = false;
          element.toggle();
          actY(element.getValue());
        }

        if (element.name.equals("Z"))
        { 
          element.active = false;
          element.toggle();
          actZ(element.getValue());
        }

        if (element.name.equals("R"))
        { 
          element.active = false;
          element.toggle();
          actR(element.getValue());
        }

        if (element.name.equals("G"))
        { 
          element.active = false;
          element.toggle();
          actG(element.getValue());
        }

        if (element.name.equals("B"))
        { 
          element.active = false;
          element.toggle();
          actB(element.getValue());
        }

        if (element.name.equals("Number"))
        { 
          element.active = false;
          element.toggle();
          actNumber(element.getValue());
        }

        if (element.name.equals("Blanking"))
        { 
          element.active = false;
          element.toggle();
          actBlanking(element.getValue());
        }

        if (element.name.equals("Palette"))
        { 
          element.active = false;
          element.toggle();
          actPalette(element.getValue());
        }

        if (element.name.equals("None"))
        { 
          element.active = false;
          actNone();
        }



        if (element.name.equals("close"))
        { 
          element.active = false;
          closeElement();
        }
      }
    }
  }

  public void addInputNode()
  {
    nodes.add(new InNode(10, 45+(++numberOfInputs)*20, "Frame " + (numberOfInputs +2), 0));
    for (GuiElement element : gui)
    {
      if (!element.name.equals("close")) element.y += 20;
    }
    newYValue += 20;
  }

  public void toggleOptions(float displayThem)
  {
    if (displayThem == 1)
    {
      for (GuiElement element : gui)
      {
        if (!element.name.equals("Options") && !element.name.equals("close")) 
        {
          element.visible = true;
        }
      }
      newYValue = 220 + 20*numberOfInputs;
      extended = true;
    } else
    {
      for (GuiElement element : gui)
      {
        if (!element.name.equals("Options") && !element.name.equals("close")) 
        {
          element.visible = false;
        }
      }
      newYValue = 80 + 20*numberOfInputs;
      extended = false;
    }
  }

  public void actX(float input)
  {
    mergOptions[2] = input;
  }

  public void actY(float input)
  {
    mergOptions[4] = input;
  }

  public void actZ(float input)
  {
    mergOptions[6] = input;
  }

  public void actR(float input)
  {
    mergOptions[3] = input;
  }

  public void actG(float input)
  {
    mergOptions[5] = input;
  }

  public void actB(float input)
  {
    mergOptions[7] = input;
  }

  public void actNumber(float input)
  {
    mergOptions[1] = input;
  }

  public void actBlanking(float input)
  {
    mergOptions[8] = input;
  }

  public void actPalette(float input)
  {
    mergOptions[9] = input;
  }

  public void actNone()
  {
    for (int i = 0; i < mergOptions.length; i++)
    {
      mergOptions[i] = 0;
    }
    for (GuiElement element : gui)
    {
      if (!element.name.equals("Options") && !element.name.equals("close") && !element.name.equals("Add node") && !element.name.equals("None")) 
      {
        element.setValue(1);
        element.active = true;
      }
    }
    guiActionDetected();
  }

  public String[] getElementAsString()
  {
    String[] superList = super.getElementAsString();
    StringList args = new StringList();
    args.append("NumberOfInputs " + numberOfInputs);

    return concat(superList, args.array());
  }
}

class Oscilloutput extends Oscillelement
{

  int displaySize = 90;
  int activeFrame = 0;
  int lastTime = 0;
  int maxFrames = 1;
  ArrayList<Frame> outputFrames = new ArrayList<Frame>(maxFrames);
  boolean record = false;
  int playbackSpeed = 30;

  Oscilloutput(int x, int y)
  {
    super(x, y, 200, 100, "Output");
    generateNodes();
    generateGui();
  }

  Oscilloutput(String[] input)
  {
    super(input);
    sizex = 200;
    sizey = 100;
    generateNodes();
    generateGui();
  }

  public void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new InNode(10, 25, "Input", 0));
    nodes.add(new InNode(10, 45, "Sync", 1));
  }

  public void display(boolean hide)
  {
    if (hide) return;

    super.display(hide);

    fill(0);
    noStroke();
    if (!hide)rect(x+sizex-displaySize-5, y+5, displaySize, displaySize);

    if (activeFrame>= 0 && activeFrame < outputFrames.size()  && !hide)
    {
      outputFrames.get(activeFrame).drawFrame(x+sizex-displaySize-5, y+5, 0, displaySize, displaySize, 0, false, true);
    }

    if (record && frameCount%90<45)
    {
      fill(255, 0, 0);
      noStroke();
      ellipse(x+sizex-displaySize+5, y+15, 5, 5);
    }

    textFont(f8);
    fill(0);
    text((activeFrame+1) + " / " + outputFrames.size(), x+5, y+sizey-12);
  }

  public void update()
  {
    super.update();
    if (millis() - lastTime > 1000/playbackSpeed && record)
    {
      activeFrame++;
      lastTime = millis();
    }
    sequenceCreator.osc.outFrames = outputFrames;
  }

  public void nodeInput(String nodeName, Frame inputFrame)
  {
    if (nodeName.equals("Input"))
    {
      if (inputFrame != null)
      {
        Frame stupidcrapJava = new Frame(inputFrame.clone());

        if (stupidcrapJava.frameName == null) stupidcrapJava.frameName = "Oscillabstract";
        if (stupidcrapJava.companyName == null) stupidcrapJava.companyName = "IldaViewer";
        stupidcrapJava.pointCount = stupidcrapJava.points.size();
        stupidcrapJava.frameNumber = activeFrame;
        stupidcrapJava.totalFrames = maxFrames;
        stupidcrapJava.scannerHead = 123;
        if (activeFrame == outputFrames.size())outputFrames.add(stupidcrapJava);
        else if (activeFrame >= 0 && activeFrame < outputFrames.size()) outputFrames.set(activeFrame, stupidcrapJava);
        else if (activeFrame < 0) activeFrame = 0;
        else if (activeFrame > outputFrames.size())
        {
          for (int i = outputFrames.size (); i < activeFrame; i++)
          {
            outputFrames.add(new Frame());
          }
          outputFrames.add(stupidcrapJava);
        }
      }
    }
  }

  public void nodeInput(String nodeName, float[] in)
  {
    if (nodeName.equals("Sync"))
    {
      if (in.length > 0) activeFrame = (int) in[0];
      if (activeFrame > maxFrames) maxFrames = activeFrame;
    }
  }

  public void generateGui()
  {
    gui.add( new GuiToggle(sizex-displaySize-70, sizey-25, 55, 20, "Record"));
  }

  public void guiActionDetected()
  {
    for (GuiElement element : gui)
    {
      if (element.active) 
      {
        if (element.name.equals("Record"))
        { 
          element.active = false;
          if (element.getValue() == 0) 
          {
            record = true;
            outputFrames.clear();
            activeFrame = 0;
          } else 
          {
            record = false;
          }
          element.setValue(1-element.getValue());
        }
      }
    }
  }
}



class Oscisource extends Oscillelement
{
  ArrayList<Frame> sources = new ArrayList<Frame>();

  int activeFrame = 0;
  int playbackSpeed = 20;
  int lastTime = 0;
  int displaySize = 90;
  float newYValue = sizey;
  boolean resizing = false;
  int xoffset = 0;
  int frameSize = 100;
  int visibleFrames = PApplet.parseInt(width/frameSize);
  int firstFrame = -1;
  int lastFrame = -1;
  boolean scrolling = false;
  int prevX = 0;
  boolean startscrolling = true;
  int xoffset2 = 0;
  int preprogFrame = -1;
  boolean scrolling2 = false;
  boolean autoplay = false;

  ArrayList<Frame> preprogframes = new ArrayList<Frame>();

  boolean selectTheFrames = false;

  Oscisource(float x, float y)
  {
    super(x, y, 100, 225, "Source");
    generateGui();
    generatePreProgFrames(200);
    generateNodes();
  }

  Oscisource(float x, float y, Frame sourceFrame)
  {
    super(x, y, 100, 225, "Source");
    sources.add(sourceFrame);
    generateGui();
    generatePreProgFrames(200);
  }

  Oscisource(float x, float y, ArrayList<Frame> sources)
  {
    super(x, y, 100, 225, "Source");
    this.sources = sources;
    generateGui();
    generatePreProgFrames(200);
  }

  Oscisource(String[] input)
  {
    super(input);
    if (sizex == 0) sizex = 100;
    if (sizey == 0) sizey = 225;
    generateGui();
    generateNodes();
    generatePreProgFrames(200);
    for (String arg : input)
    {
      String[] q = splitTokens(arg);
      if (q.length >= 2)
      {

        if (q[0].equals("Beginframe")) firstFrame = PApplet.parseInt(q[1]);
        if (q[0].equals("Endframe")) lastFrame = PApplet.parseInt(q[1]);
        if (q[0].equals("Programmedframe")) preprogFrame = PApplet.parseInt(q[1]);
        if (q[0].equals("Framefile"))
        {
          String frameName = q[1];
          if (q.length > 2)
          {
            for (int i = 2; i < q.length; i++)
            {
              frameName = frameName + " " + q[i];
            }
          }

          char[] test = new char[4];  //Test if it already has the extension .ild:
          for (int i = 0; i < 4; i++)
          {
            if (frameName.length() > 3)test[i] = frameName.charAt(i+frameName.length()-4);
          }
          String testing = new String(test);
          if ( !testing.equals(".ild") )
          {
            frameName += ".ild";
          }

          File selection = sequenceCreator.osc.openedLocation;
          String blah = selection.getPath();
          String[] fixedPath = splitTokens(blah, ".");
          String path = fixedPath[0];
          if (fixedPath.length > 1)
          {
            for (int i = 1; i < fixedPath.length-1; i++)
            {
              path = path + "." + fixedPath[i];
            }
          } 

          path = path + frameName;
          try
          {
            File f = new File(path);
            if (f.exists())
            {
              FileWrapper file = new FileWrapper(path, true);  //Create a new FileWrapper, this constructor automatically reads the file, second argument doesn't matter just to distinguish it from the other one
              ArrayList<Frame> frm = file.getFramesFromBytes();
              if (firstFrame != -1 && lastFrame != -1)
              {
                for (int i = 0; i <= min (lastFrame-firstFrame, frm.size ()-1); i++)
                {
                  if (frames != null) sources.add(frm.get(i));
                }
                lastFrame = frames.size() + lastFrame - firstFrame;
                firstFrame = frames.size();

                frames.addAll(frm);
              }
            } else
            {
              status.add("Error when trying to read frames for Source element. Make sure this file exists:");
              status.add(path);
            }
          }
          catch(Exception e)
          {
            status.add("Error when trying to read frames for Source element. Make sure this file exists:");
            status.add(path);
          }
        }

        if (q[0].equals("GUI") )
        {
          if (q[1].equals("Select frames"))
          {
          }
          if (q[1].equals("Options"))
          { 
            toggleOptions(1-PApplet.parseFloat(q[2]));
          }
          if (q[1].equals("close"))
          {
          }
          if (q[1].equals("Autoplay"))
          { 
            toggleAutoplay(PApplet.parseFloat(q[2]));
          }
        }
      }
    }
    if (preprogFrame >= 0 && preprogFrame < preprogframes.size())
    {
      sources.add(preprogframes.get(preprogFrame));
    }
  } 

  public void display(boolean hide)
  {
    super.display(sequenceCreator.osc.hideElements);
    fill(0);
    noStroke();
    if (!hide)rect(x+5, y+15, displaySize, displaySize);
    try
    {
      if (activeFrame>= 0 && activeFrame < sources.size() &&!selectTheFrames && !hide)
      {
        Frame f = sources.get(activeFrame);
        if (f != null) f.drawFrame(x+5, y+15, 0, displaySize, displaySize, 0, false, true);
      }
    }
    catch(Exception e)
    {
      println(e);
    }


    if (selectTheFrames)
    {
      if (mousePressed) sequenceCreator.osc.addNewElement = false;
      sequenceCreator.osc.hideElements = true;
      checkMouseOver = false;
      textAlign(LEFT);
      fill(50);
      noStroke();
      rect(0, 150, width, height-300);
      if (!frames.isEmpty())
      {

        for (int i = max ( (int) -xoffset/frameSize, 0); i < min(frames.size(), (int) -xoffset/frameSize+visibleFrames+1); i++)
        {
          fill(0);
          noStroke();

          if (i == firstFrame)
          {
            fill(0);
            strokeWeight(3);
            stroke(255, 255, 0);
          }


          if (i == lastFrame)
          {
            fill(0);
            strokeWeight(3);
            stroke(0, 255, 255);
          }


          if ( i< lastFrame && i > firstFrame)
          {
            strokeWeight(2); 
            stroke(lerp(0, 255, 1-((float)i-(float)firstFrame)/(lastFrame)), 255, lerp(0, 255, ((float)i-(float)firstFrame)/(lastFrame)));
          }

          fill(0);
          rect(xoffset+frameSize*i, 160, 90, 90);
          frames.get(i).drawFrame((float)xoffset+frameSize*i, 160.0f, 0.0f, 90.0f, 90.0f, 0.0f, false, true);
          if (i == firstFrame)
          {
            fill(255, 50);
            textFont(f16);
            text("First", xoffset + frameSize*i+5, 180);
          }
          if (i == lastFrame)
          {
            fill(255, 50);
            textFont(f16);
            text("Last", xoffset + frameSize*i+50, 240);
          }
        }
      } else
      {
        textFont(f16);
        fill(255, 50);
        text("No frames", 50, 190);
      }

      if (mousePressed && mouseY > 150 && mouseY < 250)
      {
        int pickedFrame = (int) (mouseX-xoffset)/frameSize;
        if (mouseButton == LEFT) firstFrame = pickedFrame;
        if (mouseButton == RIGHT) lastFrame = pickedFrame;
        if (mouseButton == CENTER && !scrolling) 
        {
          scrolling = true;
          prevX = -xoffset + mouseX;
          cursor(MOVE);
        }
        preprogFrame = -1;
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
      rect(10, 260, 35, 25);
      fill(0);
      textFont(f20);
      text("<<", 25, 278);
      if (mousePressed && mouseX > 10 && mouseX < 45 && mouseY > 260 && mouseY <285) xoffset = 5;

      fill(127);  
      rect(50, 260, 35, 25);
      fill(0);
      textFont(f20);
      text("<", 65, 278);
      if (mousePressed && mouseX > 50 && mouseX < 85 && mouseY > 260 && mouseY <285) xoffset += 5;

      fill(127);  
      rect(width-85, 260, 35, 25);
      fill(0);
      textFont(f20);
      text(">", width-67, 278);
      if (mousePressed && mouseX > width-85 && mouseX < width-50 && mouseY > 260 && mouseY <285) xoffset -= 5;

      fill(127);  
      rect(width-45, 260, 35, 25);
      fill(0);
      textFont(f20);
      text(">>", width-27, 278);
      if (mousePressed && mouseX > width-45 && mouseX < width-10 && mouseY > 260 && mouseY <285) xoffset = -frames.size() * frameSize + frameSize * visibleFrames;

      fill(127);
      float scrollX = map(xoffset, 0, -frames.size()*frameSize, 100, width-100);
      rect(scrollX, 260, 10, 25);      
      if (mousePressed && mouseX > scrollX && mouseX < scrollX+10 && mouseY > 260 && mouseY <285) startscrolling = true;
      if (startscrolling)
      {
        xoffset = (int) map(mouseX, 100, width-100, 0, (float)-frames.size()*frameSize);
        if (xoffset > 5) xoffset = 5;
        if (xoffset < -frames.size()*frameSize+ frameSize * visibleFrames) xoffset = -frames.size()*frameSize+ frameSize * visibleFrames;
      }

      if (startscrolling && !mousePressed) startscrolling = false;

      if (!preprogframes.isEmpty())
      {
        for (int i = 0; i < preprogframes.size (); i++)
        {
          fill(0);
          noStroke();
          if (i == preprogFrame)
          {

            fill(0);
            strokeWeight(3);
            stroke(255, 255, 0);
          }

          fill(0);
          rect(xoffset2+frameSize*i, 300, 90, 90);
          preprogframes.get(i).drawFrame((float)xoffset2+frameSize*i, 300, 0.0f, 90.0f, 90.0f, 0.0f, false, true);
        }
      }

      if (mousePressed && mouseY > 300 && mouseY < 400)
      {
        int pickedFrame = (int) (mouseX-xoffset2)/frameSize;
        if (mouseButton == CENTER)
        {
          scrolling2 = true;
          prevX = -xoffset2 + mouseX;
        } else
        {
          preprogFrame = pickedFrame;
          firstFrame = -1;
          lastFrame = -1;
        }
      }

      if (!mousePressed && scrolling2)
      {
        scrolling2 = false;
      }

      if (scrolling2)
      {
        xoffset2 = mouseX - prevX;
      }

      fill(127);
      noStroke();
      rect(width*0.5f- 55, height-180, 45, 20);
      rect(width*0.5f- 5, height-180, 45, 20);

      fill(0);
      textFont(f12);
      text("Accept", width*0.5f-35, height-167);
      text("Cancel", width*0.5f+15, height-167);

      //Accept
      if (mousePressed && mouseX > width*0.5f-55 && mouseX < width*0.5f-10 && mouseY > height-180 && mouseY < height-160)
      {
        selectTheFrames = false;
        checkMouseOver = true;
        sequenceCreator.osc.hideElements = false;
        sources.clear();
        try
        {
          if (firstFrame != -1 && lastFrame != -1)
          {
            for (int i = firstFrame; i <= lastFrame; i++)
            {
              sources.add(frames.get(i));
            }
          } else
          {
            if (preprogFrame != -1) sources.add(preprogframes.get(preprogFrame));
          }
        }
        catch(Exception e)
        {
        }
      }
      //Cancel
      if (mousePressed && mouseX > width*0.5f-5 && mouseX < width*0.5f+40 && mouseY > height-180 && mouseY < height-160)
      {
        selectTheFrames = false;
        checkMouseOver = true;
        sequenceCreator.osc.hideElements = false;
      }
    }
  }


  public void update()
  {       
    super.update(); 

    if (autoplay)
    {
      if (millis() - lastTime > 1000/playbackSpeed)
      {
        activeFrame++;
        if (activeFrame >= sources.size() || activeFrame < 0 ) activeFrame = 0;
        lastTime = millis();
      }
    } 
    if (resizing)
    {
      reachSize(sizex, newYValue);
      if (sizey == newYValue) resizing = false;
    }
  }

  public Frame getFrameValue(String outName)
  {
    if (outName.equals("Output"))
    {
      if (activeFrame >= 0 && activeFrame < sources.size())
      {
        return sources.get(activeFrame);
      }
    }
    return null;
  }

  public float[] getFloatArrayValue(String outName)
  {
    if (outName.equals("Sync")) 
    {
      float[] out = {
        activeFrame
      };
      return out;
    }
    float[] out = {
      0
    };
    return out;
  }

  public void guiActionDetected()
  {
    for (GuiElement element : gui)
    {
      if (element.active) 
      {
        if (element.name.equals("Select frames"))
        { 
          element.active = false;
          selectFrames();
        }
        if (element.name.equals("Options"))
        { 
          element.active = false;
          toggleOptions(element.getValue());
          element.setValue(1-element.getValue());
        }
        if (element.name.equals("close"))
        { 
          element.active = false;
          closeElement();
        }
        if (element.name.equals("Autoplay"))
        { 
          element.active = false;
          element.setValue(1-element.getValue());
          toggleAutoplay(element.getValue());
        }
      }
    }
  }

  public void generateGui()
  {
    gui.add(new GuiDropdown(5, 145, 90, 20, "Options", false));
    gui.add(new GuiButton(5, 170, 90, 20, "Select frames"));
    gui.add(new GuiToggle(5, 195, 90, 20, "Autoplay"));
    gui.add(new GuiClose(sizex-15, 0));
  }

  public void selectFrames()
  {
    mousePressed = false;
    selectTheFrames = true;
    sequenceCreator.osc.addNewElement = false;
    status.clear();
    status.add("Left click to select the first frame, right click to select the last frame.");
  }

  public void toggleOptions(float displayThem)
  {

    if (displayThem == 1)
    {
      for (GuiElement element : gui)
      {
        if (element.name.equals("Select frames")) element.visible = true;
        if (element.name.equals("Autoplay")) element.visible = true;
      }
      newYValue = 225;
    } else
    {
      for (GuiElement element : gui)
      {
        if (element.name.equals("Select frames")) element.visible = false;
        if (element.name.equals("Autoplay")) element.visible = false;
      }
      newYValue = 170;
    }
    resizing = true;
  }

  public void toggleAutoplay(float shouldIt)
  {
    if (shouldIt == 1) 
    {
      autoplay = true;
    } else autoplay = false;
  }

  public void resetNode(Node node)
  {
    if (node.name.equals("Sync_in"))
    {
      activeFrame = 0;
    }
  }

  public void generatePreProgFrames(int numPoints)
  {
    preprogframes = new ArrayList<Frame>();
    //Empty:
    Frame emptyFrame = new Frame();

    preprogframes.add(emptyFrame); 
    numPoints--; //So that an extra point can be generated to "close" the shapes, without changing the desired amount of points

    //Dot:
    Frame dotFrame = new Frame();
    ArrayList<Point> dotPoints = new ArrayList<Point>();
    for (int i = 0; i <= numPoints; i++)
    {
      dotPoints.add(new Point(width*0.5f, height*0.5f, depth*0.5f, 255, 255, 255, false));
    }
    dotFrame.points = dotPoints;
    dotFrame.frameName = "dot";
    preprogframes.add(dotFrame);

    //Line:
    Frame lineFrame = new Frame();
    ArrayList<Point> linePoints = new ArrayList<Point>();
    for (int i = 0; i <= numPoints; i++)
    {
      linePoints.add( new Point(PApplet.parseFloat(i)*width/numPoints, height*0.5f, depth*0.5f, 255, 255, 255, false));
    }
    lineFrame.points = linePoints;
    lineFrame.frameName = "line";
    preprogframes.add(lineFrame);

    //Circle:
    Frame circleFrame = new Frame();
    ArrayList<Point> somePoints = new ArrayList<Point>();
    for (int i = 0; i <= numPoints; i++)
    {
      float x = map( sin(PApplet.parseFloat(i)*TWO_PI/numPoints), -1, 1, 0, width);
      float y = map( cos(PApplet.parseFloat(i)*TWO_PI/numPoints), -1, 1, 0, height);
      somePoints.add(new Point(x, y, depth*0.5f, 255, 255, 255, false));
    }
    circleFrame.points = somePoints;
    circleFrame.frameName = "circle";
    preprogframes.add(circleFrame);

    //Triangle:
    Frame triFrame = new Frame();
    ArrayList<Point> triPoints = new ArrayList<Point>();
    for (int i = 0; i < numPoints*0.333f; i++)
    {
      float x = map( i, 0, numPoints*0.333f, 0, width);
      float y = height;
      triPoints.add(new Point(x, y, depth*0.5f, 255, 255, 255, false));
    }
    for (int i = PApplet.parseInt (numPoints*0.333f); i < PApplet.parseInt(numPoints*0.666f); i++)
    {
      float x = map( i, numPoints*0.333f, numPoints*0.666f, width, width*0.5f);
      float y = map( i, numPoints*0.333f, numPoints*0.666f, height, 0);
      triPoints.add(new Point(x, y, depth*0.5f, 255, 255, 255, false));
    }
    for (int i = PApplet.parseInt (numPoints*0.666f); i <= numPoints; i++)
    {
      float x = map( i, numPoints*0.666f, numPoints, width*0.5f, 0);
      float y = map( i, numPoints*0.666f, numPoints, 0, height);
      triPoints.add(new Point(x, y, depth*0.5f, 255, 255, 255, false));
    }
    triFrame.points = triPoints;
    triFrame.frameName = "triangle";
    preprogframes.add(triFrame);

    //Spiral:
    Frame spiralFrame = new Frame();
    ArrayList<Point> spiralPoints = new ArrayList<Point>();
    for (int i = 0; i <= numPoints; i++)
    {
      float x = map( PApplet.parseFloat(i)/numPoints*sin(PApplet.parseFloat(i)*2*TWO_PI/numPoints), -1, 1, 0, width);
      float y = map( PApplet.parseFloat(i)/numPoints*cos(PApplet.parseFloat(i)*2*TWO_PI/numPoints), -1, 1, 0, height);
      spiralPoints.add(new Point(x, y, i*depth/numPoints, 255, 255, 255, false));
    }
    spiralFrame.points = spiralPoints;
    spiralFrame.frameName = "spiral";
    preprogframes.add(spiralFrame);
  }

  public void generateNodes()
  {
    nodes.add(new InNode(10, displaySize+45, "Sync_in", 1));
    nodes.add(new OutNode(sizex-10, displaySize+25, "Output", 0));
    nodes.add(new OutNode(sizex-10, displaySize+45, "Sync", 1));
  }

  public void nodeInput(String nodeName, float[] input)
  {
    if (nodeName.equals("Sync_in"))
    {
      if (input.length > 0) activeFrame = (int) input[0];
    }
  }

  public String[] getElementAsString()
  {
    String[] superList = super.getElementAsString();
    StringList args = new StringList();
    args.append("Beginframe " + firstFrame);
    args.append("Endframe " + lastFrame);
    args.append("Programmedframe " + preprogFrame);
    if (!sources.isEmpty() && preprogFrame == -1) 
    {
      int count = 0;
      for (Frame frame : sources)
      {
        frame.frameName = "Oscillabstract";
        frame.companyName = "IldaViewer";
        frame.pointCount = frame.points.size();
        frame.frameNumber = count++;
        frame.totalFrames = sources.size();
        frame.scannerHead = 0;
      }
      String name = "_frames_" + index;
      args.append("Framefile " + name + ".ild");
      File selection = sequenceCreator.osc.openedLocation;
      String blah = selection.getPath();
      String[] fixedPath = splitTokens(blah, ".");
      String path = "";
      if (fixedPath.length > 1)
      {
        for (int i = 0; i < fixedPath.length-1; i++)
        {
          path = path + fixedPath[i];
        }
      } else path = fixedPath[0];
      path = path + name + ".ild";
      FileWrapper file = new FileWrapper(path, sources, 4);
    }

    return concat(superList, args.array());
  }
}

class Oscilloconstant extends Oscillelement
{
  float[] value = {
    0
  };
  boolean connected = false;
  boolean updateValue = false;
  float oldValue;

  Oscilloconstant(float x, float y)
  {
    super(x, y, 100, 70, "Constant");
    generateNodes();
    generateGui();
  }

  Oscilloconstant(String[] input)
  {
    super(input);
    sizex = 100;
    sizey = 70;
    generateNodes();
    generateGui();
    for (String arg : input)
    {
      String[] q = splitTokens(arg);
      if (q.length >= 2)
      {
        if (q[0].equals("Value")) value[0] = PApplet.parseFloat(q[1]);
        if (q[0].equals("GUI") && q.length >= 3 )
        {

          if (q[1].equals("Value"))
          { 
            for (GuiElement el : gui)
            {
              if (el.name.equals("Value")) el.setValue(PApplet.parseFloat(q[2]));
            }
          }
        }
      }
    }
  }

  public void update()
  {
    super.update();
    //getNode("Output").setColour((int) min(255, max(0, value[0]))*255);
    for (GuiElement el : gui)
    {
      if (el.name.equals("Value")) value[0] = el.getValue();
    }

    getNode("Output").setColour(color((int) min(255, max(0, value[0]*255))));
  }

  public void display(boolean hide)
  {
    super.display(hide);
  }

  public void generateGui()
  {
    gui.add(new GuiClose(sizex-15, 0));
    gui.add(new GuiNumberBox(5, 45, 90, 20, "Value"));
  }

  public void guiActionDetected()
  {
    for (GuiElement element : gui)
    {
      if (element.active) 
      {

        if (element.name.equals("Value"))
        { 
          element.active = true;
          /*
          if (!mousePressed) element.active = false;
           changeValue(element.getValue());
           updateValue = true;
           oldValue = value[0];
           */
        }
        if (element.name.equals("close"))
        { 
          element.active = false;
          closeElement();
        }
      }
    }
  }

  public void generateNodes()
  {
    nodes.add(new OutNode(sizex-10, 25, "Output", 1));
  }

  public void changeValue(float nr)
  {
    value[0] = nr;
    for (GuiElement el : gui)
    {
      if (el.name.equals("Value")) el.setValue(nr);
    }
  }

  public float[] getFloatArrayValue(String outName)
  {
    if (outName.equals("Output")) return value;
    return null;
  }

  public String[] getElementAsString()
  {
    String[] superList = super.getElementAsString();
    StringList args = new StringList();
    args.append("Value " + value[0]);


    return concat(superList, args.array());
  }
}

class Oscilladder extends Oscillelement
{
  ArrayList<float[]> values = new ArrayList<float[]>(2);
  boolean subtract = false;
  float[] output = {
    0
  };
  int numberOfInputs = 2;

  Oscilladder(float x, float y)
  {
    super(x, y, 100, 100, "Adder");
    generateNodes();
    generateGui();
    values.add(new float[0]);
    values.add(new float[0]);
  }

  Oscilladder(String[] input)
  {
    super(input);
    sizex = 100;
    sizey = 100;
    for (String arg : input)
    {
      String[] q = splitTokens(arg);
      if (q.length >= 2)
      {
        if (q[0].equals("Subtract")) subtract = PApplet.parseBoolean(q[1]);
        if (q[0].equals("Inputs")) numberOfInputs = PApplet.parseInt(q[1]);
      }
    }
    generateNodes();
    generateGui();

    if (subtract)
    {
      for (GuiElement element : gui)
      {
        if (element.name.equals("Add")) element.text = "Subtract";
      }
    }

    values.add(new float[0]);
    values.add(new float[0]);
    for (int i = 2; i < numberOfInputs; i++)
    {
      values.add(new float[0]);
      nodes.add(new InNode(10, 5+20*i, "Value " + (i+1), 1));
      for (GuiElement el : gui)
      {
        if (!el.name.equals("close")) el.y += 20;
      }
      sizey += 20;
    }
  }

  public void update()
  {
    super.update();
    int l = 1;

    if (!values.isEmpty()) 
    {
      for (float[] f : values)
      {
        if (l < f.length) l = f.length;
      }

      output = mapArray(l, values.get(0), 0);
      if (subtract)
      {
        if (values.size() > 1)
        {
          for (int i = 1; i < values.size (); i++)
          {
            float[] temp = mapArray(l, values.get(i), 0);
            for (int j = 0; j < temp.length; j++)
            {
              output[j] -= temp[j];
            }
          }
        }
      } else
      {
        for (int j = 1; j < values.size (); j++)
        {
          float[] input = mapArray(l, values.get(j), 0);
          if (input.length > 0)
          {
            for (int i = 0; i < l; i++)
            {
              output[i] += input[i];
            }
          }
        }
      }
    }
    for (int i = 0; i < values.size (); i++)
    {
      float[] v = values.get(i);
      if (v.length > 0) getNode("Value " + (i+1)).setColour(color((int) min(255, max(0, v[0]*255))));
    }
    if (output.length > 0) getNode("Output").setColour(color((int) min(255, max(0, output[0]*255))));
  }

  public void generateGui()
  {
    gui.add(new GuiClose(sizex-15, 0));
    gui.add(new GuiButton(sizex-50, 65, 45, 20, "Add"));
    gui.add(new GuiButton(5, 5+3*20, 20, 20, "+"));
  }

  public void generateNodes()
  {
    nodes.add(new InNode(10, 25, "Value 1", 1));
    nodes.add(new InNode(10, 45, "Value 2", 1));
    nodes.add(new OutNode(sizex-10, 25, "Output", 1));
  }

  public void nodeInput(String nodeName, float[] input)
  {
    for (int i = 0; i < numberOfInputs; i++)
    {
      if (nodeName.equals("Value " + (i+1)))
      {
        float[] val = new float[input.length];
        arrayCopy(input, val);
        if (i < values.size())values.set(i, val);
      }
    }
  }

  public void resetNode(Node node)
  {
    for (int i = 0; i < numberOfInputs; i++)
    {
      if (node.name.equals("Value " + (i+1)))
      {
        float[] val = new float[0];
        if (i < values.size())values.set(i, val);
      }
    }
  }

  public float[] getFloatArrayValue(String outName)
  {
    if (outName.equals("Output")) return output;
    return null;
  }

  public void guiActionDetected()
  {
    for (GuiElement element : gui)
    {
      if (element.active) 
      {

        if (element.name.equals("+"))
        {
          element.active = false;
          numberOfInputs++;
          nodes.add(new InNode(10, 5+20*numberOfInputs, "Value " + numberOfInputs, 1));
          for (GuiElement el : gui)
          {
            if (!el.name.equals("close")) el.y += 20;
          }
          sizey += 20;
          values.add(new float[0]);
        }
        if (element.name.equals("Add"))
        {
          if (subtract) 
          {
            element.text = "Add";
            subtract = false;
          } else 
          {
            element.text = "Subtract";
            subtract = true;
          }
          element.toggle();
        }

        if (element.name.equals("close"))
        { 
          element.active = false;
          closeElement();
        }
      }
    }
  }

  public String[] getElementAsString()
  {
    String[] superList = super.getElementAsString();
    StringList args = new StringList();
    args.append("Inputs " + numberOfInputs);
    args.append("Subtract " + subtract);

    return concat(superList, args.array());
  }
}

class Osciplier extends Oscillelement
{
  ArrayList<float[]> values = new ArrayList<float[]>(2);
  boolean divide = false;
  float[] output = {
    0
  };
  int numberOfInputs = 2;
  boolean error = false;

  Osciplier(float x, float y)
  {
    super(x, y, 100, 100, "Multiplier");
    generateNodes();
    generateGui();
    values.add(new float[0]);
    values.add(new float[0]);
  }

  Osciplier(String[] input)
  {
    super(input);
    sizex = 100;
    sizey = 100;
    for (String arg : input)
    {
      String[] q = splitTokens(arg);
      if (q.length >= 2)
      {
        if (q[0].equals("Divide")) divide = PApplet.parseBoolean(q[1]);
        if (q[0].equals("Inputs")) numberOfInputs = PApplet.parseInt(q[1]);
      }
    }
    generateNodes();
    generateGui();
    if (divide)
    {
      for (GuiElement element : gui)
      {
        if (element.name.equals("Multiply")) element.text = "Divide";
      }
    }

    values.add(new float[0]);
    values.add(new float[0]);
    for (int i = 2; i < numberOfInputs; i++)
    {
      nodes.add(new InNode(10, 5+20*(i+1), "Value " + (i+1), 1));
      for (GuiElement el : gui)
      {
        if (!el.name.equals("close")) el.y += 20;
      }
      sizey += 20;
      values.add(new float[0]);
    }
  }

  public void update()
  {
    super.update();
    int l = 1;
    error = false;

    if (!values.isEmpty()) 
    {
      for (float[] f : values)
      {
        if (l < f.length) l = f.length;
      }

      output = new float[l];
      if (divide)
      {
        if (values.size() > 0) arrayCopy(values.get(0), output);
        if (values.size() > 1)
        {
          for (int i = 1; i < values.size (); i++)
          {
            if (values.get(i).length == 0) error = true;
            float[] temp = mapArray(l, values.get(i), 1);
            for (int j = 0; j < temp.length; j++)
            {
              if (temp[j] != 0) output[j] /= temp[j];
              else output[j] = 1e32f;
            }
          }
        }
      } else
      {
        if (values.size() > 0) arrayCopy(values.get(0), output);
        if (values.size() > 1)
        {
          for (int j = 1; j < values.size (); j++)
          {
            float[] input = mapArray(l, values.get(j), 1);

            if (input.length > 0)
            {
              for (int i = 0; i < l; i++)
              {
                output[i] *= input[i];
              }
            }
          }
        }
      }
    }
    for (int i = 0; i < values.size (); i++)
    {
      float[] v = values.get(i);
      if (v.length > 0) getNode("Value " + (i+1)).setColour(color((int) min(255, max(0, v[0]*255))));
    }
    if (error) getNode("Output").setColour(color(255, 50, 10));
    if (output.length > 0) getNode("Output").setColour(color((int) min(255, max(0, output[0]*255))));
  }

  public void generateGui()
  {
    gui.add(new GuiClose(sizex-15, 0));
    gui.add(new GuiButton(sizex-50, 65, 45, 20, "Multiply"));
    gui.add(new GuiButton(5, 5+(2+1)*20, 20, 20, "+"));
  }

  public void generateNodes()
  {
    nodes.add(new InNode(10, 25, "Value 1", 1));
    nodes.add(new InNode(10, 45, "Value 2", 1));
    nodes.add(new OutNode(sizex-10, 25, "Output", 1));
  }

  public void nodeInput(String nodeName, float[] input)
  {
    for (int i = 0; i < numberOfInputs; i++)
    {
      if (nodeName.equals("Value " + (i+1)))
      {
        float[] val = new float[input.length];
        arrayCopy(input, val);
        if (i < values.size())values.set(i, val);
      }
    }
  }

  public void resetNode(Node node)
  {
    for (int i = 0; i < numberOfInputs; i++)
    {
      if (node.name.equals("Value " + (i+1)))
      {
        float[] val = new float[0];
        if (i < values.size())values.set(i, val);
      }
    }
  }

  public float[] getFloatArrayValue(String outName)
  {
    if (outName.equals("Output")) return output;
    return null;
  }

  public void guiActionDetected()
  {
    for (GuiElement element : gui)
    {
      if (element.active) 
      {

        if (element.name.equals("+"))
        {
          element.active = false;
          numberOfInputs++;
          nodes.add(new InNode(10, 5+20*numberOfInputs, "Value " + numberOfInputs, 1));
          for (GuiElement el : gui)
          {
            if (!el.name.equals("close")) el.y += 20;
          }
          sizey += 20;
          values.add(new float[0]);
        }
        if (element.name.equals("Multiply"))
        {
          if (divide) 
          {
            element.text = "Multiply";
            divide = false;
          } else 
          {
            element.text = "Divide";
            divide = true;
          }
          element.toggle();
        }

        if (element.name.equals("close"))
        { 
          element.active = false;
          closeElement();
        }
      }
    }
  }

  public String[] getElementAsString()
  {
    String[] superList = super.getElementAsString();
    StringList args = new StringList();
    args.append("Inputs " + numberOfInputs);
    args.append("Divide " + divide);

    return concat(superList, args.array());
  }
}

class Oscimath extends Oscillelement
{
  float[] ina = {
    0
  };
  float[] inb = {
    0
  };
  float[] inc = {
    0
  };
  float[] ind = {
    0
  };
  int state = 0;
  float[] output = {
    0
  };
  //String functionName = "";
  int nrOfInputs = 1;
  //int nrOfStates = 20;
  PImage formula;
  boolean selectingFormula = false;
  StringList formulaNames = new StringList();
  int sx = 100;
  int sy = 160;
  float yoffset, prevyoffset;
  int blahy = 53;

  Oscimath(float x, float y)
  {
    super(x, y, 100, 160, "Math");
    setFunctionNames();
    generateNodes();
    generateGui();
    setFunction(0);
  }

  Oscimath(String[] input)
  {
    super(input);
    sizex = 100;
    sizey = 160;
    for (String arg : input)
    {
      String[] q = splitTokens(arg);
      if (q.length >= 2)
      {
        if (q[0].equals("State")) state = PApplet.parseInt(q[1]);
      }
    }
    setFunctionNames();
    generateNodes();
    generateGui();   
    setFunction(state);
  }

  public void display(boolean hide)
  {
    super.display(hide);
    if (hide) return;

    fill(0);
    //textFont(f12);
    //textAlign(CENTER);
    //text(formulaNames.get(state), sizex/2+x, 35+y);

    if (formula != null && !selectingFormula)
    {
      imageMode(CENTER);
      image(formula, x+sizex/2, y+60);
    }
  }

  public void update()
  {
    super.update();
    reachSize(sx, sy);

    if (selectingFormula)
    {
      for (GuiElement el : gui)
      {

        if (el.name.equals("Scroller")) 
        {
          yoffset = el.getValue();
          yoffset = map(yoffset, 0, 1, 0, formulaNames.size()*11);
        }
        if (formulaNames.hasValue(el.name))
        {

          el.y  -= yoffset - prevyoffset;

          if (el.y < 41 || el.y > 135 - el.sizey) el.alpha = 00;
          else el.alpha = 255;
        }
      }
      prevyoffset = yoffset;
    }

    int l = max(nrOfInputs > 0 ? ina.length : 0, nrOfInputs > 1 ? inb.length : 0, max(nrOfInputs > 2 ? inc.length : 0, nrOfInputs > 3 ? ind.length : 0));
    output = new float[l];

    switch(state)
    {
    case 0 : 
      ina = mapArray(l, ina, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = sq(ina[i]);
      }
      break;
    case 1 : 
      ina = mapArray(l, ina, 0);
      inb = mapArray(l, inb, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = pow(ina[i], inb[i]);
      }
      break;
    case 2 : 
      ina = mapArray(l, ina, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = sqrt(ina[i]);
      }
      break;
    case 3 : 
      ina = mapArray(l, ina, 0);
      inb = mapArray(l, inb, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = pow(ina[i], 1.0f/inb[i]);
      }
      break;
    case 4 : 
      ina = mapArray(l, ina, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = exp(ina[i]);
      }
      break;
    case 5 : 
      ina = mapArray(l, ina, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = log(ina[i]);
      }
      break;
    case 6 : 
      ina = mapArray(l, ina, 0);
      inb = mapArray(l, inb, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = log(ina[i])/log(inb[i]);
      }
      break;
    case 7 : 
      ina = mapArray(l, ina, 0);
      inb = mapArray(l, inb, 0);
      inc = mapArray(l, inc, 0);
      ind = mapArray(l, ind, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = sqrt(sq(inc[i]-ina[i])+sq(ind[i]-inb[i]));
      }
      break;
    case 8 : 
      ina = mapArray(l, ina, 0);
      inb = mapArray(l, inb, 0);
      inc = mapArray(l, inc, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = exp(-inb[i]*sq(ina[i]-inc[i]));
      }
      break;
    case 9 : 
      ina = mapArray(l, ina, 0);
      inb = mapArray(l, inb, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = ina[i]%inb[i];
      }
      break;
    case 10 : 
      ina = mapArray(l, ina, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = floor(ina[i]);
      }
      break;
    case 11 : 
      ina = mapArray(l, ina, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = ceil(ina[i]);
      }
      break;
    case 12 : 
      ina = mapArray(l, ina, 0);
      inb = mapArray(l, inb, 1);
      for (int i = 0; i < l; i++)
      {
        float r = ina[i]%inb[i];
        output[i] = ina[i] + (r < inb[i]*0.5f ? -r : inb[i]-r);
      }
      break;
    case 13 : 
      ina = mapArray(l, ina, 0);
      float max = 0;
      for (int i = 0; i < ina.length; i++)
      {
        max = ina[i] > max ? ina[i] : max;
      }
      for (int i = 0; i < l; i++)
      {
        output[i] = max;
      }
      break;
    case 14 : 
      ina = mapArray(l, ina, 0);
      float min = 0;
      for (int i = 0; i < ina.length; i++)
      {
        min = ina[i] < min ? ina[i] : min;
      }
      for (int i = 0; i < l; i++)
      {
        output[i] = min;
      }
      break;
    case 15 : 
      ina = mapArray(l, ina, 0);
      inb = mapArray(l, inb, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = ina[i] > inb[i] ? ina[i] : inb[i];
      }
      break;
    case 16 : 
      ina = mapArray(l, ina, 0);
      inb = mapArray(l, inb, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = ina[i] < inb[i] ? ina[i] : inb[i];
      }
      break;
    case 17 : 
      ina = mapArray(l, ina, 0);
      inb = mapArray(l, inb, 0);
      inc = mapArray(l, inc, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = constrain(ina[i], inb[i], inc[i]);
      }
      break;
    case 18 : 
      ina = mapArray(l, ina, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = abs(ina[i]);
      }
      break;
    case 19 : 
      ina = mapArray(l, ina, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = sign(ina[i]);
      }
      break;
    case 20 : 
      ina = mapArray(l, ina, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = sin(ina[i]);
      }
      break;
    case 21 : 
      ina = mapArray(l, ina, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = cos(ina[i]);
      }
      break;
    case 22 : 
      ina = mapArray(l, ina, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = tan(ina[i]);
      }
      break;
    case 23 : 
      ina = mapArray(l, ina, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = 1f/cos(ina[i]);
      }
      break;
    case 24 : 
      ina = mapArray(l, ina, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = 1f/sin(ina[i]);
      }
      break;
    case 25 : 
      ina = mapArray(l, ina, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = 1f/tan(ina[i]);
      }
      break;
    case 26 : 
      ina = mapArray(l, ina, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = asin(ina[i]);
      }
      break;
    case 27 : 
      ina = mapArray(l, ina, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = acos(ina[i]);
      }
      break;
    case 28 : 
      ina = mapArray(l, ina, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = atan(ina[i]);
      }
      break;
    case 29 : 
      ina = mapArray(l, ina, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = 0.5f*(exp(ina[i])-exp(-ina[i]));
      }
      break;
    case 30 : 
      ina = mapArray(l, ina, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = 0.5f*(exp(ina[i])+exp(-ina[i]));
      }
      break;
    case 31 : 
      ina = mapArray(l, ina, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = (exp(ina[i])-exp(-ina[i]))/( exp(ina[i])+exp(-ina[i]));
      }
      break;
    case 32 : 
      ina = mapArray(l, ina, 0);
      inb = mapArray(l, inb, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = atan2(inb[i], ina[i]);
      }
      break;
    case 33 : 
      ina = mapArray(l, ina, 0);
      //float d = 1.0/l;
      if (l > 1)
      {
        for (int i = 1; i < l; i++)
        {
          output[i] = (ina[i] - ina[i-1]);
        }
        if (l > 2) output[l-1] = output[l-2];
      }
      break;
    case 34 : 
      ina = mapArray(l, ina, 0);
      float s = 0;
      for (int i = 0; i < l; i++)
      {
        s += ina[i];
        output[i] = s;
      }
      break;
    }

    if (output.length > 0) getNode("Output").setColour(color((int) min(255, max(0, output[0]*255))));
  }

  public void setFunctionNames()
  {
    formulaNames.append("Square");
    formulaNames.append("Power");
    formulaNames.append("Sqrt");
    formulaNames.append("n-th root");
    formulaNames.append("exp");
    formulaNames.append("nat. log");
    formulaNames.append("log base");
    formulaNames.append("distance");
    formulaNames.append("Gaussian");
    formulaNames.append("modulo");
    formulaNames.append("floor");
    formulaNames.append("ceiling");
    formulaNames.append("round");
    formulaNames.append("max");
    formulaNames.append("min");
    formulaNames.append("largest");
    formulaNames.append("smallest");
    formulaNames.append("constrain");
    formulaNames.append("abs");
    formulaNames.append("sign");
    formulaNames.append("sin");
    formulaNames.append("cos");
    formulaNames.append("tan");
    formulaNames.append("sec");
    formulaNames.append("cosec");
    formulaNames.append("cot");
    formulaNames.append("asin");
    formulaNames.append("acos");
    formulaNames.append("atan");
    formulaNames.append("sinh");
    formulaNames.append("cosh");
    formulaNames.append("tanh");
    formulaNames.append("atan2");
    formulaNames.append("diff");
    formulaNames.append("sum");
  }

  public void setFunction(int input)
  {
    for (GuiElement el : gui)
    {
      if (el.name.equals("Formula")) el.text = formulaNames.get(input);
    }
    try
    {
      //Images generated using http://www.tlhiv.org/ltxpreview/
      formula = loadImage("/Images/math/" + input + ".png");
      if (formula.height > 30) formula.resize(0, 30);
      if (formula.width > 97) formula.resize(97, 0);
    }
    catch(Exception e)
    {
      println("Error: could not load formula shape " + input + ": " + formulaNames.get(input));
    }
    status.clear();
    switch(input)
    {
    case 0 : 
      nrOfInputs = 1;
      status.add("The square of a number.");
      break;
    case 1 : 
      nrOfInputs = 2;
      status.add("Input a to the power of input b.");
      break;
    case 2 : 
      nrOfInputs = 1;
      status.add("The square root of a number.");
      break;
    case 3 : 
      nrOfInputs = 2;
      status.add("The b-th root of input a.");
      break;
    case 4 : 
      nrOfInputs = 1;
      status.add("The exponential function.");
      break;
    case 5 : 
      nrOfInputs = 1;
      status.add("The natural logarithm, or the logarithm with base e.");
      break;
    case 6 : 
      nrOfInputs = 2;
      status.add("Logarithm of a with base b.");
      break;
    case 7 : 
      nrOfInputs = 4;
      status.add("The distance between two points (a,b) and (c,d). a and b are the x and y coordinates of the first point and c and d the coordinates of the second point.");
      status.add("Use the XYZ2RThetaPhi element to calculate the distance (R) between two points in 3D (using the Origin coordinates as the second point).");
      break;
    case 8 : 
      nrOfInputs = 3;
      status.add("The Gaussian curve or clock curve. b is the inverse width and c the horizontal position.");
      break;
    case 9 : 
      nrOfInputs = 2;
      status.add("The remainder of the division of a by b.");
      break;
    case 10 : 
      nrOfInputs = 1;
      status.add("Round a decimal number down to the nearest integer smaller than it.");
      break;
    case 11 : 
      nrOfInputs = 1;
      status.add("Round a decimal number up to the nearest integer larger than it.");
      break;
    case 12 : 
      nrOfInputs = 2;
      status.add("Rounds a to the nearest integer multiple or b.");
      break;
    case 13 : 
      nrOfInputs = 1;
      status.add("Finds the largest number of an input with more than one value");
      break;
    case 14 : 
      nrOfInputs = 1;
      status.add("Finds the smallest number of an input with more than one value");
      break;
    case 15 : 
      nrOfInputs = 2;
      status.add("Finds the largest value of the two inputs a and b, numbers are compared per-value rather than over the complete value stream.");
      break;
    case 16 : 
      nrOfInputs = 2;
      status.add("Finds the smallest value of the two inputs a and b, numbers are compared per-value rather than over the complete value stream.");
      break;
    case 17 : 
      nrOfInputs = 3;
      status.add("The output is equal to a, but a can't go lower than b or higher than c.");
      break;
    case 18 : 
      nrOfInputs = 1;
      status.add("The absolute value of a.");
      break;
    case 19 : 
      nrOfInputs = 1;
      status.add("The sign of a: -1 when a < 0, 1 when a > 0 and 0 when a = 0.");
      break;
    case 20 : 
      nrOfInputs = 1;
      status.add("sine");
      break;
    case 21 : 
      nrOfInputs = 1;
      status.add("cosine");
      break;
    case 22 : 
      nrOfInputs = 1;
      status.add("tangent");
      break;
    case 23 : 
      nrOfInputs = 1;
      status.add("secant");
      break;
    case 24 : 
      nrOfInputs = 1;
      status.add("cosecant");
      break;
    case 25 : 
      nrOfInputs = 1;
      status.add("cotangent");
      break;
    case 26 : 
      nrOfInputs = 1;
      status.add("arcsine");
      break;
    case 27 : 
      nrOfInputs = 1;
      status.add("arccosine");
      break;
    case 28 : 
      nrOfInputs = 1;
      status.add("arctangent");
      break;
    case 29 : 
      nrOfInputs = 1;
      status.add("hyperbolic sine");
      break;
    case 30 : 
      nrOfInputs = 1;
      status.add("hyperbolic cosine");
      break;
    case 31 : 
      nrOfInputs = 1;
      status.add("hyperbolic tangent");
      break;
    case 32 : 
      nrOfInputs = 2;
      status.add("Modified arctangent: returns the angle (in radians) between the point (a,b), the origin (0,0) and the x axis.");
      break;
    case 33 : 
      nrOfInputs = 1;
      status.add("Calculates the difference between consecutive values.");
      break;
    case 34 : 
      nrOfInputs = 1;
      status.add("Cumulatively sums all values together.");
      break;
    default:
      //functionName = "";
      nrOfInputs = 0;
      break;
    }
    for (int i = 0; i < 4; i++)
    {
      if (i < nrOfInputs) getNode(str((char) (i+97))).setColour(color(0));
      else getNode(str((char) (i+97))).setColour(color(100, 50, 50));
    }
  }

  public void generateGui()
  {
    gui.add(new GuiClose(sizex-15, 0));
    gui.add(new GuiButton(5, 20, 15, 20, "<-"));
    gui.add(new GuiButton(sizex-20, 20, 15, 20, "->"));
    GuiButton f = new GuiButton(25, 20, 50, 20, "Formula");
    f.text = formulaNames.get(0);
    gui.add(f);
    GuiScroller sc = new GuiScroller(112, 42, 8, 95, "Scroller");
    sc.visible = false;
    gui.add(sc );
    int i = 0;
    for (String s : formulaNames)
    {
      GuiButton b = new GuiButton(5+(i%2)*52, 42+22*((i++)/2), 50, 20, s);
      b.visible = false;
      gui.add(b);
    }
  }

  public void generateNodes()
  {
    nodes.add(new InNode(10, 85, "a", 1));
    nodes.add(new InNode(10, 105, "b", 1));
    nodes.add(new InNode(10, 125, "c", 1));
    nodes.add(new InNode(10, 145, "d", 1));
    nodes.add(new OutNode(sizex-10, 85, "Output", 1));
  }

  public void nodeInput(String nodeName, float[] input)
  {

    if (nodeName.equals("a"))
    {
      ina = input;
    }
    if (nodeName.equals("b"))
    {
      inb = input;
    }
    if (nodeName.equals("c"))
    {
      inc = input;
    }
    if (nodeName.equals("d"))
    {
      ind = input;
    }
  }

  public void resetNode(Node node)
  {
    if (node.name.equals("a"))
    {
      ina = new float[1];
    }
    if (node.name.equals("b"))
    {
      inb = new float[1];
    }
    if (node.name.equals("c"))
    {
      inc = new float[1];
    }
    if (node.name.equals("d"))
    {
      ind = new float[1];
    }
  }

  public float[] getFloatArrayValue(String outName)
  {
    if (outName.equals("Output")) return output;
    return null;
  }

  public void guiActionDetected()
  {
    boolean blah = false;
    for (GuiElement element : gui)
    {
      if (element.active) 
      {
        if (element.name.equals("<-"))
        {
          state--;
          if (state < 0) state = formulaNames.size()-1;
          setFunction(state);
          element.active = false;
        }
        if (element.name.equals("->"))
        {
          state++;
          if (state >= formulaNames.size()) state = 0;
          setFunction(state);
          element.active = false;
        }
        if (element.name.equals("Formula") && !selectingFormula)
        {
          blah = true;
          selectingFormula = true;
          sx = 125;
          sy = 220;
          //yoffset = 0;
          for (Node node : nodes)
          {
            node.y+=60;
            if (node instanceof OutNode) node.x+=25;
          }
          for (GuiElement el : gui)
          {
            if (formulaNames.hasValue(el.name)) el.visible = true;
            if (el.name.equals("Scroller")) 
            {
              el.visible = true;
              el.setValue(0);
              yoffset = 0;
              prevyoffset = 0;
            }
          }

          element.active = false;
        }
        if (selectingFormula && (formulaNames.hasValue(element.name) || element.name.equals("Formula")) && !blah)
        {
          for (int i = 0; i < formulaNames.size (); i++)
          {
            if (element.name.equals(formulaNames.get(i)))
            {
              state = i;
              setFunction(state);
            }
          }
          for (GuiElement e : gui)
          {
            if (formulaNames.hasValue(e.name)) 
            {
              e.visible = false;
              e.y += yoffset;
            }
            if (e.name.equals("Scroller")) e.visible = false;
          }


          selectingFormula = false;
          sx = 100;
          sy -= 60;
          //yoffset = 0;
          for (Node node : nodes)
          {
            node.y -= 60;
            if (node instanceof OutNode) node.x-=25;
          }
          element.active = false;
        }

        if (element.name.equals("close"))
        { 
          element.active = false;
          closeElement();
        }
      }
    }
  }

  public String[] getElementAsString()
  {
    String[] superList = super.getElementAsString();
    StringList args = new StringList();
    args.append("State " + state);

    return concat(superList, args.array());
  }
}

class Oscilogic extends Oscillelement
{
  ArrayList<float[]> inputs = new ArrayList<float[]>();
  ArrayList<Boolean> connected = new ArrayList<Boolean>();
  int state = 0;
  float[] output = {
    0
  };
  int nrOfInputs = 1;
  int inputAmount = 0;
  PImage formula;
  boolean selectingFormula = false;
  StringList formulaNames = new StringList();
  int sx = 100;
  int sy = 100;
  float yoffset, prevyoffset;
  int blahy = 53;

  Oscilogic(float x, float y)
  {
    super(x, y, 100, 100, "Logic");
    setFunctionNames();
    generateGui();
    generateNodes();


    setFunction(0);
  }

  Oscilogic(String[] input)
  {
    super(input);
    sizex = 100;
    sizey = 100;
    int addNodes = 0;
    for (String arg : input)
    {
      String[] q = splitTokens(arg);
      if (q.length >= 2)
      {
        if (q[0].equals("State")) state = PApplet.parseInt(q[1]);
        if (q[0].equals("Inputs")) addNodes = max(0, PApplet.parseInt(q[1])-3);
      }
    }
    setFunctionNames();
    generateGui(); 
    generateNodes();

    for (int i = 0; i < addNodes; i++)
    {
      addInNode();
    }  
    setFunction(state);
  }

  public void display(boolean hide)
  {
    super.display(hide);
    if (hide) return;

    if (nrOfInputs != -1)
    {

      for (int i = nrOfInputs; i < inputs.size (); i++)
      {
        getNode(generateInputString(i)).setColour(color(100, 50, 50));
      }
    } else
    {
      for (int i = 0; i < inputs.size (); i++)
      {
        getNode(generateInputString(i)).setColour(0);
      }
    }


    if (selectingFormula) return;

    fill(0);
    textAlign(CENTER);
    rectMode(CENTER);
    textFont(f16);
    textLeading(16);

    float textx = x + sizex*0.5f;
    float texty = y+65;

    switch(state)
    {
    case 0 : 
      text("If a then b else c", textx, texty, sizex-10, 45);
      break;
    case 1 : 
      textFont(f12);
      text("If a = 0 then b, if a = 1 then c, ...", textx, texty, sizex-10, 45);
      break;
    case 2 : 
      text("a && b && c ...", textx, texty, sizex-10, 45);
      break;
    case 3 : 
      text("a || b || c ...", textx, texty, sizex-10, 45);
      break;
    case 4 : 
      text("(a || b) && !(a && b)", textx, texty, sizex-10, 45);
      break;
    case 5 : 
      text("!a", textx, texty, sizex-10, 45);
      break;
    case 6 : 
      text("a < {b, c, ... }", textx, texty, sizex-10, 45);
      break;
    case 7 : 
      text("a > {b, c, ... }", textx, texty, sizex-10, 45);
      break;
    case 8 : 
      text("a \u2264 {b, c, ... }", textx, texty, sizex-10, 45);
      break;
    case 9 : 
      text("a \u2265 {b, c, ... }", textx, texty, sizex-10, 45);
      break;
    case 10 : 
      text("a == {b, c, ... }", textx, texty, sizex-10, 45);
      break;
    case 11 : 
      text("a != {b, c, ... }", textx, texty, sizex-10, 45);
      break;
    case 12 : 
      text("a in [b, c]", textx, texty, sizex-10, 45);
      break;
    case 13 : 
      text("a in {b, c, ... }", textx, texty, sizex-10, 45);
      break;
    }
    rectMode(CORNER);
  }

  public void update()
  {
    super.update();
    reachSize(sx, sy);

    if (selectingFormula)
    {
      for (GuiElement el : gui)
      {

        if (el.name.equals("Scroller")) 
        {
          yoffset = el.getValue();
          yoffset = map(yoffset, 0, 1, 0, formulaNames.size()*11);
        }
        if (formulaNames.hasValue(el.name))
        {

          el.y  -= yoffset - prevyoffset;

          if (el.y < 41 || el.y > 135 - el.sizey) el.alpha = 00;
          else el.alpha = 255;
        }
      }
      prevyoffset = yoffset;
    }

    int l = 1;
    for (float[] f : inputs)
    {
      if (f.length > l) l = f.length;
    }
    output = new float[l];

    switch(state)
    {
    case 0 : 
      float[] ina = mapArray(l, inputs.get(0), 0);
      float[] inb = mapArray(l, inputs.get(1), 0);
      float[] inc = mapArray(l, inputs.get(2), 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = ina[i] >= 1 ? inb[i] : inc[i];
      }
      break;

    case 1 : 
      ina = mapArray(l, inputs.get(0), 0);

      for (int i = 0; i < l; i++)
      {
        output[i] = mapArray(l, inputs.get( constrain((int)ina[i]+1, 1, inputs.size()-1)), 0)[i];
      }
      break;
    case 2 : 

      float[] first = mapArray(l, inputs.get(0), 0);
      for (int i = 0; i < l; i++) output[i] = first[i] >= 1 ? 1 : 0;
      for (int j = 1; j < inputs.size (); j++)
      {
        float[] blub = mapArray(l, inputs.get(j), 0);
        for (int i = 0; i < l; i++)
        {
          if (connected.get(j)) output[i] *= blub[i] >= 1 ? 1 : 0;
        }
      }
      break;
    case 3 : 
      for (int j = 0; j < inputs.size (); j++)
      {
        float[] blub = mapArray(l, inputs.get(j), 0);
        for (int i = 0; i < l; i++)
        {
          if (connected.get(j)) output[i] = constrain(output[i] + (blub[i] >= 1 ? 1 : 0), 0, 1);
        }
      }
      break;
    case 4 :

      for (int j = 0; j < inputs.size (); j++)
      {
        float[] blub = mapArray(l, inputs.get(j), 0);
        for (int i = 0; i < l; i++)
        {
          if (connected.get(j)) output[i] += blub[i] >= 1 ? 1 : 0 ;
        }
      }

      for (int i = 0; i < l; i++)
      {
        output[i] = output[i] > 1 ? 0 : output[i];
      } 
      break;

    case 5 : 
      ina = mapArray(l, inputs.get(0), 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = ina[i] >= 1 ? 0 : 1;
      }
      break;

    case 6 : 
      ina = mapArray(l, inputs.get(0), 0);
      boolean noneConnected = true;
      for (int i = 0; i < l; i++)
      {
        output[i] = 1;
      }
      for (int j = 1; j < inputs.size (); j++)
      {
        if (connected.get(j)) noneConnected = false;
        if (!noneConnected)
        {
          float[] ploenk = mapArray(l, inputs.get(j), 0);
          for (int i = 0; i < l; i++)
          {
            if (connected.get(j)) output[i] = ina[i] < ploenk[i] ? 1*output[i] : 0;
          }
        }
      }
      if (noneConnected) output = new float[1];
      break;

    case 7 : 
      ina = mapArray(l, inputs.get(0), 0);
      noneConnected = true;
      for (int i = 0; i < l; i++)
      {
        output[i] = 1;
      }
      for (int j = 1; j < inputs.size (); j++)
      {
        if (connected.get(j)) noneConnected = false;
        if (!noneConnected)
        {
          float[] ploenk = mapArray(l, inputs.get(j), 0);
          for (int i = 0; i < l; i++)
          {
            if (connected.get(j)) output[i] = ina[i] > ploenk[i] ? 1*output[i] : 0;
          }
        }
      }
      if (noneConnected) output = new float[1];
      break;

    case 8 : 
      ina = mapArray(l, inputs.get(0), 0);
      noneConnected = true;
      for (int i = 0; i < l; i++)
      {
        output[i] = 1;
      }
      for (int j = 1; j < inputs.size (); j++)
      {
        if (connected.get(j)) noneConnected = false;
        if (!noneConnected)
        {
          float[] ploenk = mapArray(l, inputs.get(j), 0);
          for (int i = 0; i < l; i++)
          {
            if (connected.get(j)) output[i] = ina[i] <= ploenk[i] ? 1*output[i] : 0;
          }
        }
      }
      if (noneConnected) output = new float[1];
      break;

    case 9 : 
      ina = mapArray(l, inputs.get(0), 0);
      noneConnected = true;
      for (int i = 0; i < l; i++)
      {
        output[i] = 1;
      }
      for (int j = 1; j < inputs.size (); j++)
      {
        if (connected.get(j)) noneConnected = false;
        if (!noneConnected)
        {
          float[] ploenk = mapArray(l, inputs.get(j), 0);
          for (int i = 0; i < l; i++)
          {
            if (connected.get(j)) output[i] = ina[i] >= ploenk[i] ? 1*output[i] : 0;
          }
        }
      }
      if (noneConnected) output = new float[1];
      break;

    case 10 : 
      ina = mapArray(l, inputs.get(0), 0);
      noneConnected = true;
      for (int i = 0; i < l; i++)
      {
        output[i] = 1;
      }
      for (int j = 1; j < inputs.size (); j++)
      {
        if (connected.get(j)) noneConnected = false;
        if (!noneConnected)
        {
          float[] ploenk = mapArray(l, inputs.get(j), 0);
          for (int i = 0; i < l; i++)
          {
            if (connected.get(j)) output[i] = ina[i] == ploenk[i] ? 1*output[i] : 0;
          }
        }
      }
      if (noneConnected) output = new float[1];
      break;

    case 11 : 
      ina = mapArray(l, inputs.get(0), 0);
      noneConnected = true;
      for (int i = 0; i < l; i++)
      {
        output[i] = 1;
      }
      for (int j = 1; j < inputs.size (); j++)
      {
        if (connected.get(j)) noneConnected = false;
        if (!noneConnected)
        {
          float[] ploenk = mapArray(l, inputs.get(j), 0);
          for (int i = 0; i < l; i++)
          {
            if (connected.get(j)) output[i] = ina[i] != ploenk[i] ? 1*output[i] : 0;
          }
        }
      }
      if (noneConnected) output = new float[1];
      break;

    case 12 : 
      ina = mapArray(l, inputs.get(0), 0);
      inb = mapArray(l, inputs.get(1), 0);
      inc = mapArray(l, inputs.get(2), 0);

      for (int i = 0; i < l; i++)
      {
        output[i] = (ina[i] >= inb[i] && ina[i] <= inc[i]) || (ina[i] >= inc[i] && ina[i] <= inb[i]) ? 1 : 0;
      }
      break;

    case 13 : 
      ina = mapArray(l, inputs.get(0), 0);

      for (int j = 1; j < inputs.size (); j++)
      {

        float[] ploenk = mapArray(l, inputs.get(j), 0);
        for (int i = 0; i < l; i++)
        {
          if (connected.get(j)) output[i] = ina[i] == ploenk[i] ? 1 : output[i];
        }
      }
      break;
    }


    for (boolean b : connected)
    {
      b = false;
    }


    if (output.length > 0 ) 
    {
      if (state != 0 && state != 1)getNode("Output").setColour(output[0] >= 1 ? color(25, 250, 25) : color(250, 25, 25));
      else getNode("Output").setColour( color((int) min(255, max(0, output[0]*255))));
    }
  }

  public void setFunctionNames()
  {
    formulaNames.append("If");
    formulaNames.append("Switch");
    formulaNames.append("And");
    formulaNames.append("Or");
    formulaNames.append("xOr");
    formulaNames.append("Not");
    formulaNames.append("<");
    formulaNames.append(">");
    formulaNames.append("\u2264");
    formulaNames.append("\u2265");
    formulaNames.append("=");
    formulaNames.append("\u2260");
    formulaNames.append("In range");
    formulaNames.append("Element of");
  }

  public void setFunction(int input)
  {
    for (GuiElement el : gui)
    {
      if (el.name.equals("Formula")) el.text = formulaNames.get(input);
    }

    status.clear();
    switch(input)
    {
    case 0 : 
      nrOfInputs = 3;
      status.add("If a is equal to or greater than 1, output is b, else output is c.");
      break;
    case 1 : 
      nrOfInputs = -1;
      status.add("If a is between 0 and 1, output is b; if a is between 1 and 2, output is c, etc.");
      break;
    case 2 : 
      nrOfInputs = -1;
      status.add("Are all inputs greater than or equal to one?");
      break;
    case 3 : 
      nrOfInputs = -1;
      status.add("Is at least one input greater than or equal to one?");
      break;
    case 4 : 
      nrOfInputs = -1;
      status.add("Is at most one input greater than or equal to one?");
      break;
    case 5 : 
      nrOfInputs = 1;
      status.add("Output is 0 if a is 1 and 1 if a is 0.");
      break;
    case 6 : 
      nrOfInputs = -1;
      status.add("Is a smaller than all other inputs?");
      break;
    case 7 : 
      nrOfInputs = -1;
      status.add("Is a larger than all other inputs?");
      break;
    case 8 : 
      nrOfInputs = -1;
      status.add("Is a smaller than or equal to all other inputs?");
      break;
    case 9 : 
      nrOfInputs = -1;
      status.add("Is a larger than or equal to all other inputs?");
      break;
    case 10 : 
      nrOfInputs = -1;
      status.add("Are all inputs equal?");
      break;
    case 11 : 
      nrOfInputs = -1;
      status.add("Is a different from all other inputs?");
      break;
    case 12 : 
      nrOfInputs = 3;
      status.add("Is a inside the interval [b,c]?");
      break;
    case 13 : 
      nrOfInputs = -1;
      status.add("Is a an element of another input?");
      break;

    default:
      //functionName = "";
      nrOfInputs = 0;
      break;
    }
  }

  public void generateGui()
  {
    gui.add(new GuiClose(sizex-15, 0));
    gui.add(new GuiButton(5, 20, 15, 20, "<-"));
    gui.add(new GuiButton(sizex-20, 20, 15, 20, "->"));
    GuiButton f = new GuiButton(25, 20, 50, 20, "Formula");
    f.text = formulaNames.get(0);
    gui.add(f);
    GuiScroller sc = new GuiScroller(112, 42, 8, 95, "Scroller");
    sc.visible = false;
    gui.add(sc );
    int i = 0;
    for (String s : formulaNames)
    {
      GuiButton b = new GuiButton(5+(i%2)*52, 42+22*((i++)/2), 50, 20, s);
      b.visible = false;
      gui.add(b);
    }
    gui.add(new GuiButton(5, sizey-25, sizex-40, 20, "Add input"));
  }

  public void generateNodes()
  {
    addInNode();
    addInNode();
    addInNode();

    nodes.add(new OutNode(sizex-10, 85, "Output", 1));
  }

  public void addInNode()
  {
    float[] empty = {
      0
    };
    inputs.add(empty);
    connected.add(false);
    sy += 20;
    for (GuiElement el : gui)
    {
      if (el.name.equals("Add input")) el.y += 20;
    }
    nodes.add(new InNode(10, 85+20*inputAmount, generateInputString(inputAmount++), 1));
  }

  public String generateInputString(int input)
  {
    String output = "";

    if (input < 26)
    { 
      output += (char) (97+input%26);
    } else if (input == 26) output +=  "" + ((char) 97) + ((char) 97);
    else
    {
      int q = (int) (log(input-26)/log(26));
      float rem = input-26;
      for (int i = q; i >= 0; i--)
      {
        float p = pow(26, i);
        float c = rem/p;
        rem = rem - (int) c*p;

        output += (input < 52 ? "a" : "") + (char) (97 + (int) c);
      }
    }
    return output;
  }

  public int calculateInputString(String input)
  {
    if (input.length() == 1)
    {
      return (int) input.charAt(0)-97;
    } else
    {
      int output = 0;
      for (int i = 0; i < input.length (); i++)
      {
        output += ((int) input.charAt(i)-96) * pow(26, input.length()-i-1);
      }
      return output-1;
    }
  }

  public void nodeInput(String nodeName, float[] input)
  {
    int pos = calculateInputString(nodeName);
    connected.set(pos, true);
    if (pos < inputs.size())
    {
      inputs.set(pos, java.util.Arrays.copyOf(input, input.length));
      if (input.length > 0)
      {
        if (pos == 0 && (state == 0 )) getNode(nodeName).setColour(input[0] >= 1 ? color(25, 250, 25) : color(250, 25, 25));
        else if (state == 1)
        {
          if (pos == 0) getNode(nodeName).setColour(color((int) min(255, max(0, input[0]*255))));
          else 
          {
            if (inputs.get(0).length > 0) getNode(nodeName).setColour((int) inputs.get(0)[0] == (pos-1) ? color(25, constrain(map(input[0], 0, 1, 25, 250), 25, 250), 25) : color(constrain(map(input[0], 0, 1, 25, 250), 25, 250), 25, 25));
          }
        } else if (state > 1 && state < 6) getNode(nodeName).setColour(input[0] >= 1 ? color(25, 250, 25) : color(250, 25, 25));
        else getNode(nodeName).setColour(color((int) min(255, max(0, input[0]*255))));
      }
    }
  }

  public void resetNode(Node node)
  {
    int pos = calculateInputString(node.name);
    if (pos < inputs.size())
    {
      inputs.set(pos, new float[0]);
    }
    node.setColour(0);
  }

  public float[] getFloatArrayValue(String outName)
  {
    if (outName.equals("Output")) return output;
    return null;
  }

  public void guiActionDetected()
  {
    boolean blah = false;
    for (GuiElement element : gui)
    {
      if (element.active) 
      {
        if (element.name.equals("Add input"))
        {
          addInNode();
          element.active = false;
        }
        if (element.name.equals("<-"))
        {
          state--;
          if (state < 0) state = formulaNames.size()-1;
          setFunction(state);
          element.active = false;
        }
        if (element.name.equals("->"))
        {
          state++;
          if (state >= formulaNames.size()) state = 0;
          setFunction(state);
          element.active = false;
        }
        if (element.name.equals("Formula") && !selectingFormula)
        {
          blah = true;
          selectingFormula = true;
          sx = 125;
          sy += 60;
          //yoffset = 0;
          for (Node node : nodes)
          {
            node.y+=60;
            if (node instanceof OutNode) node.x+=25;
          }
          for (GuiElement el : gui)
          {
            if (formulaNames.hasValue(el.name)) el.visible = true;
            if (el.name.equals("Scroller")) 
            {
              el.visible = true;
              el.setValue(0);
              yoffset = 0;
              prevyoffset = 0;
            }
          }

          element.active = false;
        }
        if (selectingFormula && (formulaNames.hasValue(element.name) || element.name.equals("Formula")) && !blah)
        {
          for (int i = 0; i < formulaNames.size (); i++)
          {
            if (element.name.equals(formulaNames.get(i)))
            {
              state = i;
              setFunction(state);
            }
          }
          for (GuiElement e : gui)
          {
            if (formulaNames.hasValue(e.name)) 
            {
              e.visible = false;
              e.y += yoffset;
            }
            if (e.name.equals("Scroller")) e.visible = false;
          }


          selectingFormula = false;
          sx = 100;
          sy -= 60;
          //yoffset = 0;
          for (Node node : nodes)
          {
            node.y -= 60;
            if (node instanceof OutNode) node.x-=25;
          }
          element.active = false;
        }

        if (element.name.equals("close"))
        { 
          element.active = false;
          closeElement();
        }
      }
    }
  }

  public String[] getElementAsString()
  {
    String[] superList = super.getElementAsString();
    StringList args = new StringList();
    args.append("State " + state);
    args.append("Inputs " + inputAmount);

    return concat(superList, args.array());
  }
}

class Oscilloclock extends Oscillelement
{
  float[] value = {
    0
  };
  boolean connected = false;
  float count = 0;
  float[] output = new float[1];
  float frequency = 1;
  float lastTime = 0;
  float sync = 0;
  float prevsync = 0;

  Oscilloclock(float x, float y)
  {
    super(x, y, 100, 90, "Clock");
    generateNodes();
    generateGui();
  }

  Oscilloclock(String[] input)
  {
    super(input);
    sizex = 100;
    sizey = 90;
    generateNodes();
    generateGui();
    for (String arg : input)
    {
      String[] q = splitTokens(arg);
      if (q.length >= 2)
      {
        if (q[0].equals("Count")) count = PApplet.parseFloat(q[1]);
        if (q[0].equals("Frequency")) frequency = PApplet.parseFloat(q[1]);
      }
    }
  }

  public void update()
  {
    super.update();
    if (frameRate != 0) count += max(-frameRate, min(frequency/frameRate, frameRate));
    if (count >= 1 || count <= -1) count  = 0;
    if (sync != prevsync && sync >= 1)
    {
      count = 0;
      prevsync = sync;
    }
    if (!connected || value.length == 0)
    {
      output[0] = count;
    } else
    {
      if (value.length == 1) output[0] = value[0];
      int i = (int) (abs(count) * value.length);
      if (count > 0) 
      {
        output[0] = map((count*value.length)%1, 0, 1, value[i], value[min(value.length-1, i+1)]);
        //output[0] = map(count%1, 0, 1, value[], value[(int) min(count * value.length+1, value.length-1)]);
      } else output[0] = map((count*value.length)%1, -1, 0, value[i], value[min(value.length-1, i+1)]);
    }

    getNode("Output").setColour(color((int) min(255, max(0, output[0]*255))));
  }

  public void display(boolean hide)
  {
    super.display(hide);
  }

  public void generateGui()
  {
    gui.add(new GuiClose(sizex-15, 0));
  }
  public void guiActionDetected()
  {
    for (GuiElement element : gui)
    {
      if (element.active) 
      {
        if (element.name.equals("close"))
        { 
          element.active = false;
          closeElement();
        }
      }
    }
  }

  public void generateNodes()
  {
    nodes.add(new InNode(10, 45, "Frequency", 1));
    nodes.add(new InNode(10, 25, "Reset", 1));
    nodes.add(new InNode(10, 65, "Shape", 1));
    nodes.add(new OutNode(sizex-10, 25, "Output", 1));
  }

  public void nodeInput(String nodeName, float[] input)
  {
    if (nodeName.equals("Frequency"))
    {
      if (input.length > 0) frequency = input[0];
    }
    if (nodeName.equals("Reset"))
    {
      if (input.length > 0) 
      {
        sync = input[0];
      }
    }
    if (nodeName.equals("Shape"))
    {
      value = input;
      connected = true;
    }
  }

  public void resetNode(Node node)
  {
    if (node.name.equals("Frequency")) frequency = 1;
    if (node.name.equals("Shape")) connected = false;
  }

  public float[] getFloatArrayValue(String outName)
  {
    if (outName.equals("Output")) return output;
    return null;
  }

  public String[] getElementAsString()
  {
    String[] superList = super.getElementAsString();
    StringList args = new StringList();
    args.append("Count " + count);
    args.append("Frequency " + frequency);

    return concat(superList, args.array());
  }
}

class Oscillator extends Oscillelement
{

  float waveform = 0;

  float[] frequency = {
    1
  };
  float[] phase = {
    0
  };
  float[] amplitude = {
    0.5f
  };
  float[] offset = {
    0
  };
  float[] random = {
    0
  };
  float[] prevrandom = {
    0
  };
  float[] lastTime = {
    0
  };
  //int prevTime = 0;
  float samples = 200;

  float[] invalues;
  boolean inputDisconnected;

  float[] outvalues;


  Oscillator(float x, float y)
  {
    super(x, y, 100, 220, "Oscillator");

    generateNodes();
    generateGui();
  }

  Oscillator(String[] input)
  {
    super(input);
    sizex = 100;
    sizey = 220;
    generateNodes();
    generateGui();
    for (String arg : input)
    {
      String[] q = splitTokens(arg);
      if (q.length >= 2)
      {
        if (q[0].equals("Waveform")) waveform = PApplet.parseFloat(q[1]);
        if (q[0].equals("Frequency")) 
        {
          frequency = new float[1];
          frequency[0] = PApplet.parseFloat(q[1]);
        }
        if (q[0].equals("Amplitude")) 
        {
          amplitude =  new float[1];
          amplitude[0] =   PApplet.parseFloat(q[1]);
        }
        if (q[0].equals("Phase")) 
        {
          phase =  new float[1];
          phase[0] =   PApplet.parseFloat(q[1]);
        }
        if (q[0].equals("Offset")) 
        {
          offset =  new float[1];
          offset[0] =   PApplet.parseFloat(q[1]);
        }
        if (q[0].equals("Samples")) samples = PApplet.parseFloat(q[1]);

        if (q[0].equals("GUI") )
        {
        }
      }
    }
  }


  public void update()
  {
    super.update();
    outvalues = oscillate(invalues);
    if (outvalues.length > 0) getNode("Output").setColour(color((int) min(255, max(0, outvalues[0]*255))));
    inputDisconnected = true;
  }

  public void display(boolean hide)
  {
    super.display(hide);
    if (hide) return;
    fill(0);
    noStroke();
    rect(x+5, y+15, sizex-10, 40);
    if (outvalues.length > 0)
    {
      float prev = outvalues[0];
      if (laserboyMode) stroke(color(PApplet.parseInt((sin(PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(PI/2+PApplet.parseFloat(frameCount)/10)*0.5f+0.5f)*255), PApplet.parseInt((sin(3*PI/2+PApplet.parseFloat(frameCount)/20)*0.5f+0.5f)*255))); 
      else stroke(10, 127, 50);
      strokeWeight(1);
      for (int i = 1; i < outvalues.length; i++)
      {
        line(x+5+(i-1)*(sizex-10)/(outvalues.length-1), 20*max(-1, min(1, prev))+y+35, x+5+(i)*(sizex-10)/(outvalues.length-1), y+35+20*max(-1, min(1, outvalues[i])));
        prev = outvalues[i];
      }
    }
    if (mouseClicked && mouseX > x+5 && mouseX < x+sizex-5 && mouseY > y+15 && mouseY < y+55) 
    {
      if (mouseButton == LEFT) waveform++;
      else waveform--;
      if (waveform > 7) waveform = 0;
      if (waveform < 0) waveform = 7;

      mousePressed = false;
    }
  }

  public float[] oscillate(float[] input)
  {
    if (samples == 0) samples = 1;
    int l  = (int) samples;
    if (l < 1) l = 1;

    if (inputDisconnected || input == null || input.length == 0)
    {
      input = new float[l];
      for (int i = 0; i < l; i++)
      {
        input[i] = PApplet.parseFloat(i)/(l - ((waveform == 0 || waveform == 1) ? 1 : 0));
      }
    }

    float[] output = mapArray(l, input, 1);

    frequency = mapArray(l, frequency, 1);
    phase = mapArray(l, phase, 0);
    amplitude = mapArray(l, amplitude, 1);
    offset = mapArray(l, offset, 0);

    l--;

    switch((int) waveform)
    {
    case 0:    //Sine
      for (int i = 0; i <= l; i++)
      {
        output[i] = sin((output[i]*frequency[i]+phase[i])*TWO_PI)*amplitude[i]+offset[i];
      }
      break;
    case 1:     //Cosine
      for (int i = 0; i <= l; i++)
      {
        output[i] = cos((output[i]*frequency[i]+phase[i])*TWO_PI)*amplitude[i]+offset[i];
      }
      break;
    case 2:    //Linear
      for (int i = 0; i <= l; i++)
      {
        output[i] = (ramp((output[i]*frequency[i]+phase[i])))*amplitude[i]+offset[i];
      }
      break;
    case 3:    //Triangle
      for (int i = 0; i <= l; i++)
      {
        output[i] = tri((output[i]*frequency[i]+phase[i]))*amplitude[i]+offset[i];
      }
      break;
    case 4:    //Linear reversed
      for (int i = 0; i <= l; i++)
      {
        output[i] = (-ramp((output[i]*frequency[i]+phase[i])))*amplitude[i]+offset[i];
      }
      break;
    case 5:    //Square
      for (int i = 0; i <= l; i++)
      {
        output[i] = square((output[i]*frequency[i]+phase[i]))*amplitude[i]+offset[i];
      }
      break;
    case 6:    //Random
      try
      {
        float time = 0;
        if (prevrandom.length != l+1) 
        {
          prevrandom = mapArray(l+1, prevrandom, 0);
        }
        if (random.length != l+1) 
        {
          random = mapArray(l+1, random, 0);
        }
        if (lastTime.length != l+1) 
        {
          lastTime = mapArray(l+1, lastTime, 0);
        }
        if (frequency.length > 0 )
        {
          for (int i = 0; i <= l; i++)
          {
            if (millis() - lastTime[i] > 1000/abs(frequency[i]))
            {
              prevrandom[i] = random[i];

              random[i] = random(-1, 1);
              lastTime[i] = millis();
            }

            time = map(millis(), lastTime[i], lastTime[i] + 1000/abs(frequency[i]), 0, 1);
            output[i] = amplitude[i]*map(time, 0, 1, prevrandom[i], random[i])+offset[i];
          }
        }
      }
      catch(Exception e)  //???
      {
        println("Random generator exception. l: " + l + " Output size: " + output.length + ", previous size: " + prevrandom.length);
        println(e);
      }
      break;
    case 7:    //Noise

      random = mapArray(l+1, random, 0);
      prevrandom = mapArray(l+1, prevrandom, 0);

      for (int i = 0; i <= l; i++)
      {
        prevrandom[i] += phase[i];
        random[i] = noise(l*0.03f*i+prevrandom[i]*0.5f);//, random[i]*l*0.01);
      }

      for (int i = 0; i <= l; i++)
      {
        output[i] = amplitude[i]*(random[i]-0.5f)*2+offset[i];
      }
      break;
    default:
      break;
    }

    return output;
  }



  public void generateGui()
  {
    gui.add(new GuiClose(sizex-15, 0));
  }

  public void guiActionDetected()
  {
    for (GuiElement element : gui)
    {
      if (element.active) 
      {

        if (element.name.equals("Options"))
        { 
          /*
          element.active = false;
           toggleOptions(element.getValue());
           element.setValue(1-element.getValue());
           */
        }
        if (element.name.equals("close"))
        { 
          element.active = false;
          closeElement();
        }
      }
    }
  }

  public void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float) except I only use float[] and not float
    nodes.add(new InNode(10, 75, "Input", 1));
    nodes.add(new InNode(10, 95, "Waveform", 1));
    nodes.add(new InNode(10, 115, "Frequency", 1));
    nodes.add(new InNode(10, 135, "Phase", 1));
    nodes.add(new InNode(10, 155, "Amplitude", 1));
    nodes.add(new InNode(10, 175, "Offset", 1));
    nodes.add(new InNode(10, 195, "Samples", 1));
    nodes.add(new OutNode(sizex-10, 75, "Output", 1));
  }

  public void resetNode(Node node)
  {
    if (node.name.equals("Waveform")) waveform = 0;
    if (node.name.equals("Frequency")) frequency = new float[] { 
      1
    };
    if (node.name.equals("Phase")) phase = new float[] {
      0
    };
    if (node.name.equals("Amplitude")) amplitude = new float[] {
      0.5f
    };
    if (node.name.equals("Offset"))offset = new float[] { 
      0
    };
    if (node.name.equals("Samples")) samples = 200;
  }

  public void nodeInput(String nodeName, float[] input)
  {
    if (nodeName.equals("Input"))
    {
      invalues = input;
      inputDisconnected = false;
    }
    if (input.length>0)
    {
      if (nodeName.equals("Waveform"))waveform = input[0];
      if (nodeName.equals("Samples"))samples = input[0];
    }
    if (nodeName.equals("Frequency"))frequency = input;
    if (nodeName.equals("Phase"))phase = input;
    if (nodeName.equals("Amplitude"))amplitude = input;
    if (nodeName.equals("Offset"))offset = input;
  }



  public float[] getFloatArrayValue(String outName)
  {
    if (outName.equals("Output")) return outvalues;
    return null;
  }

  public String[] getElementAsString()
  {
    String[] superList = super.getElementAsString();
    StringList args = new StringList();
    args.append("Waveform " + waveform);
    args.append("Frequency " + frequency[0]);
    args.append("Phase " + phase[0]);
    args.append("Amplitude " + amplitude[0]);
    args.append("Offset " + offset[0]);
    args.append("Samples " + samples);

    return concat(superList, args.array());
  }
}

public float tri(float in)
{
  in = in%1;
  if (in>=0.5f ) return 2*ramp(in)-1;
  if (in >= 0&& in < 0.5f) return -2*ramp(in)-1;
  if (in < 0 && in > -0.5f) return 2*ramp(in)-1;
  else return -2*ramp(in)-1;
}

public float ramp(float in)
{
  if (in>=0) return 2*(in%1)-1;
  else
  {
    return 1-2*(abs(in)%1);
  }
}

public float square(float in)
{
  if (in>=0)
  {
    if ((in%1)<0.5f) return -1;
    else return 1;
  } else
  {
    if (abs(in%1)<0.5f) return 1;
    else return -1;
  }
}

public float sign(float in)
{
  if (in == 0) return 0;
  if (in < 0) return -1;
  return 1;
}

public float[] mapArray(int newLength, float[] input, float defaultValue)
{
  if (newLength < 0) return null;
  if (input == null || input.length == 0)
  {
    input = new float[newLength];
    for (int i = 0; i < newLength; i++)
    {
      input[i] = defaultValue;
    }
  }

  float[] output = new float[newLength];
  //if (input.length == newLength) return input;
  if (input.length == 1)
  {
    for (int i = 0; i < newLength; i++)
    {
      output[i] = input[0];
    }
    return output;
  }
  if (input.length == newLength)
  {
    for (int i = 0; i < newLength; i++)
    {
      output[i] = input[i];
    }
    return output;
  }

  for (int i = 0; i < newLength; i++)
  {
    float pos = map(i, 0, newLength, 0, input.length-1);
    int index = floor(pos);
    float magic = pos - index;

    output[i] = map(magic, 0, 1, input[index], input[min(index+1, input.length-1)]);
  }

  return output;
}

public float roundToHalfInt(float input)
{
  float decimalPart = input%1;
  float integerPart = input - decimalPart;

  //Positive values:
  if (decimalPart < 0.25f && decimalPart >= 0) return integerPart;
  if (decimalPart >= 0.25f && decimalPart < 0.75f) return integerPart + 0.5f;
  if (decimalPart >= 0.75f && decimalPart < 1) return integerPart +1;

  //Negative values:
  if (decimalPart > -0.25f && decimalPart <= 0) return integerPart;
  if (decimalPart <= -0.25f && decimalPart > -0.75f) return integerPart - 0.5f;
  if (decimalPart <= -0.75f && decimalPart > -1) return integerPart -1;

  return integerPart;
}




/*
* Here's how we'll do it:
 * Each Oscillelement has a few nodes (in and out)
 * Each node has a continuously updating value (float, Frame etc)
 * They also have a number referring to a Connection
 * When updated, they look for their Connection and give/retrieve the value
 * 
 */

/*
 * Change of plan
 * When an OutNode has a Connection, it writes its value to an external global array specific for his value
 * (there will be an array for each object/primitive type)
 * The Connection keeps track of the position in the array then tells the InNode to read it
 *
 */

/*
 * Funny how neither got implemented.
 * A Connection keeps track of which elements and nodes it's connected to.
 * Then it calls the getFloatArrayValue or getFrameValue according to its type from the out element node 
 * and passes the result on to the input of the other element.
 * The result is not stored somewhere in between.
 *
 */

class Node
{
  float x, y;
  String name;
  boolean visible = true;
  boolean active = false;
  int colour = color(0);
  int type; //0 = Frame, 1 = float[], 2 = float

  Node(float x, float y, String name, int type)
  {
    this.x = x;
    this.y = y;
    this.name = name;
    this.type = type;
  }

  public void update()
  {
  }

  public void connect(int index)
  {
  }

  public void setColour(int colour)
  {
    this.colour = colour;
  }

  public void display(float elx, float ely)
  {
    if (laserboyMode) fill(color(PApplet.parseInt((sin((x)/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((y)+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((elx+ely)/2+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255))); 
    else fill(colour);
    quad(x+elx-5, y+ely, x+elx, y+ely+5, x+elx+5, y+ely, x+elx, y+ely-5);
    textFont(f10);
  }

  public boolean checkMouse(float elx, float ely)
  {
    if (mouseX > x + elx -5 && mouseX < x + elx + 5 && mouseY > y + ely -5 && mouseY < y + ely + 5)
    {
      active = true;
      return true;
    }
    return false;
  }
}

class InNode extends Node
{
  InNode(float x, float y, String name, int type)
  {
    super(x, y, name, type);
  }

  public void display(float elx, float ely)
  {
    if (type == 0)
    {
      stroke(20, 100, 255);
      strokeWeight(1);
    } else noStroke();
    if (laserboyMode) fill(color(PApplet.parseInt((sin((x)/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((y)+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((elx+ely)/2+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255))); 
    else fill(0);
    textAlign(LEFT);
    text(split(name, '_')[0], x+elx+8, y+ely+3);
    fill(colour);
    super.display(elx, ely);
  }
}

class OutNode extends Node
{
  OutNode(float x, float y, String name, int type)
  {
    super(x, y, name, type);
  }

  public void display(float elx, float ely)
  {
    if (type == 0)
    {
      stroke(20, 100, 255);
      strokeWeight(1);
    } else noStroke();
    if (laserboyMode) fill(color(PApplet.parseInt((sin((x)/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((y)+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin((elx+ely)/2+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255))); 
    else fill(0);
    textAlign(RIGHT);
    text(split(name, '_')[0], x+elx-8, y+ely+3);
    fill(colour);
    super.display(elx, ely);
  }
}



class Connection
{
  int type;  //0 = Frame, 1 = float[], 2 = float
  //int index;
  int inIndex, outIndex;  //Index of the Oscillelement
  String inName, outName; //Name of the Node
  float x1, y1, x2, y2; //1 = out, 2 = in
  boolean active;
  boolean shouldUpdate;
  boolean dragInput = false;
  boolean dragOutput = false;
  boolean deleteConnection = false;
  int number = 0;  //when connecting multiple connections at the same time, which is this connection
  int numConns = 0;  //For creating multiple connections when hitting shift

  Connection(int type)
  {
    this.type = type;
    active = false;
    shouldUpdate = false;
  }

  Connection(int type, int outIndex, int inIndex, String outName, String inName)
  {
    this.type = type;
    this.inIndex = inIndex;
    this.outIndex = outIndex;
    this.inName = inName;
    this.outName = outName;
    active = false;
    shouldUpdate = true;
  }

  Connection(int type, float x1, float y1, int outIndex, String outName)
  {
    this.type = type;
    this.outIndex = outIndex;
    this.outName = outName;
    this.x1 = x1;
    this.y1 = y1;
    active = false;
    shouldUpdate = false;
  }

  Connection(int type, float x2, float y2, String inName, int inIndex)
  {
    this.type = type;
    this.inIndex = inIndex;
    this.inName = inName;
    this.x2 = x2;
    this.y2 = y2;
    active = false;
    shouldUpdate = false;
  }

  Connection(String[] input)
  {
    for (String arg : input)
    {
      String[] q = splitTokens(arg);
      if (q.length >= 2)
      {
        if (q[0].equals("Type") ) type = PApplet.parseInt(q[1]);
        if (q[0].equals("InIndex") ) inIndex = PApplet.parseInt(q[1]);
        if (q[0].equals("OutIndex") ) outIndex = PApplet.parseInt(q[1]);
        if (q[0].equals("NodeInName") )
        {
          inName = q[1];
          for (int i = 2; i < q.length; i++)
          {
            inName = inName + " " + q[i];
          }
        }
        if (q[0].equals("NodeOutName") )
        {
          outName = q[1];
          for (int i = 2; i < q.length; i++)
          {
            outName = outName + " " +q[i];
          }
        }
      }
    }
    shouldUpdate = true;
  }

  public void update()
  {
    if (shouldUpdate)
    {
      if (outName == null || inName == null) return;
      if (sequenceCreator.osc == null) return;
      Oscillelement outElement = sequenceCreator.osc.searchElement(outIndex);
      Oscillelement inElement = sequenceCreator.osc.searchElement(inIndex);
      if (outElement != null && inElement != null)
      {
        try
        {
          if (outElement.getNode(outName) == null || inElement.getNode(inName) == null) deleteConnection = true;
          x1 = outElement.getNode(outName).x + outElement.x;
          y1 = outElement.getNode(outName).y + outElement.y;
          x2 = inElement.getNode(inName).x + inElement.x;
          y2 = inElement.getNode(inName).y + inElement.y;

          switch(type)
          {
          case 0: 
            inElement.nodeInput(inName, outElement.getFrameValue(outName));
            break;
          case 1:
            inElement.nodeInput(inName, outElement.getFloatArrayValue(outName));
            break;
          default:
            break;
          }
        }
        catch(Exception e)
        {
          println("Error when updating connection from element " + outIndex + " and node " + outName + " to element " + inIndex + " and node " + inName);
          println(e);
          deleteConnection = true;
        }
      } else deleteConnection = true;
    }

    //Drag from output to input
    if (dragInput && mousePressed)
    {
      dragInput(mouseX, mouseY+20*number);
      if (keyHit && keyCode == SHIFT)
      {
        Oscillelement outElement = sequenceCreator.osc.searchElement(outIndex);
        Node outNode = outElement.searchNode(x1, y1+20*numConns+20);
        if (outNode != null)
        {
          numConns++;
          Connection connection = new Connection(outNode.type, outNode.x+outElement.x, outNode.y+outElement.y, outElement.index, outNode.name); //OutputNode fixed
          connection.number = numConns;
          connection.startDraggingInput();
          sequenceCreator.osc.connections.add(connection);
        }
        keyHit = false;  //beware...
      }
    }

    if (dragInput && !mousePressed)
    {
      dragInput = false;
      active = false;

      Oscillelement inputElement = sequenceCreator.osc.searchElement(mouseX, mouseY+20*number);
      if (inputElement != null)
      {
        Node inputNode = inputElement.searchNode(mouseX, mouseY+20*number);
        boolean alreadyConnected = false;
        if (inputNode != null)
        {
          for (Connection connection : sequenceCreator.osc.connections)
          {

            if ( connection.connectedToInput(inputElement.index, inputNode)) alreadyConnected = true;
          }
          if (alreadyConnected)
          {
            deleteConnection = true;
            return;
          }
          if (inputNode.type == type && inputNode instanceof InNode)
          {
            inName = inputNode.name;
            inIndex = inputElement.index;
            shouldUpdate = true;
          } else deleteConnection = true;
        } else deleteConnection = true;
      } else deleteConnection = true;
      number = 0;
    }

    if (dragOutput && mousePressed)
    {
      dragOutput(mouseX, mouseY+20*number);
      if (keyHit && keyCode == SHIFT)
      {
        Oscillelement inElement = sequenceCreator.osc.searchElement(inIndex);
        Node inNode = inElement.searchNode(x2, y2+20*numConns+20);

        if (inNode != null)
        {
          numConns++;
          if (inNode instanceof InNode) 
          {
            //Check if it already has an input (you can't have two connections in the same input): 
            boolean connected = false;
            for (Connection connection : sequenceCreator.osc.connections)
            {
              if (connection.connectedToInput(inElement.index, inNode)) 
              {
                connected = true;
              }
            }
            if (!connected) 
            {
              Connection connection = new Connection(inNode.type, inNode.x+inElement.x, inNode.y+inElement.y, inNode.name, inElement.index); //InputNode fixed
              connection.number = numConns;
              connection.startDraggingOutput();
              sequenceCreator.osc.connections.add(connection);
              keyHit = false;  //beware...
            }
          }
        }
      }
    }

    if (dragOutput && !mousePressed)
    {
      dragOutput = false;
      active = false;

      Oscillelement outputElement = sequenceCreator.osc.searchElement(mouseX, mouseY+20*number);
      if (outputElement != null)
      {
        Node outputNode = outputElement.searchNode(mouseX, mouseY+20*number);

        if (outputNode != null && outputNode.type == type && outputNode instanceof OutNode)
        {
          outName = outputNode.name;
          outIndex = outputElement.index;
          shouldUpdate = true;
        } else deleteConnection = true;
      } else deleteConnection = true;
      number = 0;
    }
  }



  public void display()
  {
    if (sequenceCreator.osc.hideElements) return;
    noFill();
    if (active)
    {
      stroke(255, 127, 0);
      strokeWeight(2);
    } else
    {
      strokeWeight(1);
      if (type == 0)
      {
        stroke(20, 100, 255);
      } else stroke(255);
    }
    bezier(x1, y1, x1+0.5f*abs(x2-x1), y1, x2-0.5f*abs(x2-x1), y2, x2, y2);
  }

  public void dragInput(float x, float y)
  {
    active = true;
    x2 = x;
    y2 = y;
  }

  public void connectInput(int inIndex, Node node)
  { 
    this.inIndex = inIndex;
    x2 = node.x;
    y2 = node.y;
    type = node.type;
    inName = node.name;
  }



  public void dragOutput(float x, float y)
  {
    active = true;
    x1 = x;
    y1 = y;
  }

  public void connectOutput(int outIndex, Node node)
  {
    this.inIndex = outIndex;
    x1 = node.x;
    y1 = node.y;
    type = node.type;
    outName = node.name;
  }


  public void setCoordinates(float elx, float ely, Node node)  //Node = Output
  {
    if (node == null) return;
    x1 = elx + node.x;
    y1 = ely + node.y;
  }

  public void setCoordinates( Node node, float elx, float ely)  //Node = Input
  {
    if (node == null) return;
    x2 = elx + node.x;
    y2 = ely + node.y;
  }

  public void startDraggingInput()
  {
    shouldUpdate = false;
    dragInput = true;
  }

  public void startDraggingOutput()
  {
    shouldUpdate = false;
    dragOutput = true;
  }

  public boolean connectedToInput(int index, Node node)
  {
    if (inName == null) return false;
    try
    {
      if (inIndex == index && inName.equals(node.name) && node instanceof InNode)
      {
        return true;
      }
    }
    catch(Exception e)
    {
      println("Error happened lolz");
      println(e);
    }
    return false;
  }

  public boolean connectedToOutput(int index, Node node)
  {
    if (outIndex == index && outName.equals(node.name) && node instanceof OutNode)
    {
      return true;
    } else return false;
  }

  public String[] getConnectionAsString()
  {
    StringList output = new StringList();
    output.append("Type " + type);
    output.append("InIndex " + inIndex);
    output.append("OutIndex " + outIndex);
    output.append("NodeInName " + inName);
    output.append("NodeOutName " + outName);

    return output.array();
  }
}
/*
class UnsavedDialog extends OverlayImage
 {
 ArrayList<GuiElement> gui = new ArrayList<GuiElement>();
 UnsavedDialog(float x, float y)
 {
 super(x, y);
 text = "Unsaved workspace";
 img = createGraphics(200, 150);
 gui.add(new GuiButton(5, 125, 60, 25, "Save"));
 gui.add(new GuiButton(70, 125, 60, 25, "Discard"));
 gui.add(new GuiButton(135, 125, 60, 25, "Cancel"));
 }
 
 void display(boolean top)
 {
 if (top) update();
 img.beginDraw();
 img.background(127);
 img.fill(255, 0, 0);
 img.textFont(f20);
 img.text("Warning!", 10, 25);
 img.fill(0);
 img.textFont(f16);
 img.text("The current workspace is unsaved. Do you wish to discard it?", 10, 35, 200, 90);
 img.endDraw();
 
 for (GuiElement element : gui)
 {
 element.display(img, 0, 0);
 }
 
 super.display(top);
 }
 
 void update()
 {
 for (GuiElement element : gui)
 {
 element.update(x, y+25);
 }
 
 if (mouseClicked)
 {
 for (GuiElement element : gui)
 {
 if (element.activateElement(x, y+25) && element.visible)
 {
 guiActionDetected();
 }
 }
 }
 }
 
 void guiActionDetected()
 {
 
 for (GuiElement element : gui)
 {
 
 if (element.active) 
 {
 println(element.name);
 if (element.name.equals("Save"))
 {
 element.active = false;
 finished = true;
 }
 if (element.name.equals("Discard"))
 {
 element.active = false;
 element.setValue(1-element.getValue());
 }
 if (element.name.equals("Cancel"))
 {
 element.active = false;
 element.setValue(1-element.getValue());
 }
 }
 }
 }
 }
 */
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

public void beginSeqEditor()
{
  sequenceditor = true;
  if (seqeditor == null) seqeditor = new SequenceEditor();
  else seqeditor.init();

  status.clear();
  status.add("Drag frames around. Hold ctrl to copy, right mouse button to multiselect.");
}

public void exitSeqEditor()
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

public void editorShowBlanking(boolean theValue)
{
  showBlanking = theValue;
}

public void insertFrame()
{
  seqeditor.addFrame();
  status.clear();
  status.add("Empty frames created.");
  status.add("Drag the frames to the Main frame list or the Buffer and drop them.");
}

public void deleteFrame()
{
  seqeditor.deleteFrame();
  status.clear();
  status.add("Deleted a frame.");
}

public void revrs()    //reverse was already taken
{
  seqeditor.reverseFrame();
}

public void randomize()
{
  seqeditor.randomizeFrame();
}

public void editorLoadIldaFile()
{
  //loading = true;    //It's possible frame displaying is in a separate thread, and you can't load and display at the same time
  thread("addIldaFile");
}

public void addIldaFile()
{
  selectInput("Load an Ilda file", "addFile");
}



//Gets called when an ilda file is selected in the previous method
public void addFile(File selection) {

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
public void loadAnIldaFile(String path)
{

  FileWrapper file = new FileWrapper(path, true);  //Create a new FileWrapper, this constructor automatically reads the file, second argument doesn't matter just to distinguish it from the other one
  ArrayList<Frame> newFrames = file.getFramesFromBytes();
  for (Frame frame : newFrames)
  {
    seqeditor.loadedFrames.add(new FrameDraggable(frame));
  }
  status.add("Loading completed.");
}

public void editorClearFrames()
{
  seqeditor.theFrames.clear();
  status.clear();
  status.add("All frames cleared.");
}

public void editorClearBuffer()
{
  seqeditor.bufferFrames.clear();
  status.clear();
  status.add("Buffer frames cleared.");
}

public void editorFitPalette()
{
  seqeditor.fitPalette();
  status.clear();
  status.add("Fitted RGB values with palette " + getActivePalette().name);
}

public void editorSelectFrames()
{
  seqeditor.selectFrames();
  status.clear();
  status.add("All frames selected.");
}

public void editorSelectBuffer()
{
  seqeditor.selectBuffer();
  status.clear();
  status.add("All frames in buffer selected.");
}

public void editorDeselectFrames()
{
  seqeditor.deselectFrames();
  status.clear();
  status.add("All frames deselected.");
}

public void editorDeselectBuffer()
{
  seqeditor.deselectBuffer();
  status.clear();
  status.add("All frames in buffer deselected.");
}

public void graphicView(boolean view)
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
  int visibleFrames = PApplet.parseInt((height-30)/frameSize);
  int mainColour = color(127);
  int bufferColour = color(127);
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

  public void init()
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

  public void update()
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
      stroke(color(PApplet.parseInt((sin(PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*127+127), PApplet.parseInt((sin(PI+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*127+127), 127));
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
      if (laserboyMode) fill(color(PApplet.parseInt((sin(PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255))); 
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

    int colour = color(64, 64, 64);

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



    if (abs(offvel) < 0.25f) offvel = 0;
    yoffset += offvel;
    offvel = 0.95f*offvel;
    yoffsetbuf += offvelbuf;
    offvelbuf = 0.95f*offvelbuf;

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

  public boolean shouldDisplayMain()
  {
    return ((dragging && !draggedFrames.isEmpty()) && mouseX > 20 && mouseX < 140 || !theFrames.isEmpty());
  }

  public int getCopyBehaviourColour()
  {
    int colour = color(64);
    switch(PApplet.parseInt(copyBehaviour.getValue()))
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

  public void displayMain()
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

  public boolean shouldDisplayBuffer()
  {
    return ((dragging && !draggedFrames.isEmpty()) && mouseX > 160 && mouseX < 280 || !bufferFrames.isEmpty());
  }

  public void displayBuffer()
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

  public void drag()
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
          if ( !(yoffset < -frameSize*(theFrames.size()-visibleFrames/1.5f))) yoffset -= 2*(mouseY-height+20);
        }

        //Scroll the remaining frames down when inserting:
        if (copyBehaviour.getValue() == 0)
        {
          for (int k = 0; k < theFrames.size (); k++)
          {
            if (mouseY >= frameSize*(k+0.5f)+yoffset && mouseY < frameSize*(k+1.5f)+yoffset)
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

          int pickedFrame = PApplet.parseInt((mouseY - yoffset)/frameSize-0.5f);
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
          if ( !(yoffsetbuf < -frameSize*(bufferFrames.size()-visibleFrames/1.5f))) yoffsetbuf -= 2*(mouseY-height+20);
        }

        if (copyBehaviour.getValue() == 0)
        {
          for (int k = 0; k < bufferFrames.size (); k++)
          {
            if (mouseY >= frameSize*(k+0.5f)+yoffsetbuf && mouseY < frameSize*(k+1.5f)+yoffsetbuf)
            {
              for (int j = k; j < bufferFrames.size (); j++)
              {
                bufferFrames.get(j).reachValue(frameSize * draggedFrames.size());
              }
            }
          }
        } else
        {
          int pickedFrame = PApplet.parseInt((mouseY - yoffsetbuf)/frameSize-0.5f);
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

  public void droppedOverMain()
  {
    boolean wasEmpty = false;
    if (theFrames.isEmpty()) wasEmpty = true;


    //int pickedFrame = (int) (mouseY-(frameSize+5)-yoffset)/frameSize;
    int pickedFrame = PApplet.parseInt((mouseY - yoffset)/frameSize-0.5f);

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

  public void droppedOverBuffer()
  {
    boolean wasEmpty = false;
    if (bufferFrames.isEmpty()) wasEmpty = true;
    int pickedFrame = PApplet.parseInt( (mouseY-yoffsetbuf)/frameSize-0.5f);

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


  public void addFrame()
  {
    int amount = PApplet.parseInt(cp5.get(Textfield.class, "nrOfInsertedFrames").getText());
    if (amount > 4096) amount = 4096;
    int colour = color(64, 64, 64);
    switch(PApplet.parseInt(copyBehaviour.getValue()))
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

  public void deleteFrame()
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

  public void reverseFrame()
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

  public void randomizeFrame()
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

  public void reverseFrames(ArrayList<FrameDraggable> inFrames)
  {
    loadedFrames.clear();
    for (int i = 0; i < inFrames.size (); i++)
    {
      loadedFrames.add(inFrames.get(inFrames.size()-i-1));
    }
  }

  public void randomizeFrames(ArrayList<FrameDraggable> inFrames)
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

  public void toggleHighlighted(int i)
  {
    highlightedFrame = i;
  }

  public void toggleHighlightedBuf(int i)
  {    
    highlightedFrameBuf = i;
  }

  public void fitPalette()
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

  public void update3D()
  {
    if (displayedFrame != null) displayedFrame.drawFrame(showBlanking);
  }

  public void selectFrames()
  {
    for (FrameDraggable frame : theFrames)
    {
      frame.active = true;
    }
  }

  public void selectBuffer()
  {
    for (FrameDraggable frame : bufferFrames)
    {
      frame.active = true;
    }
  }

  public void deselectFrames()
  {
    for (FrameDraggable frame : theFrames)
    {
      frame.active = false;
    }
  }

  public void deselectBuffer()
  {
    for (FrameDraggable frame : bufferFrames)
    {
      frame.active = false;
    }
  }

  public void setGraphics(boolean view)
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
    visibleFrames = PApplet.parseInt((height-30)/frameSize);
  }
}

class Draggable
{
  float yOffset;
  boolean active;
  float yOffVel;
  int sizeX=120;
  int sizeY;
  int colour;
  int originalPosition;
  int source;

  public Draggable()
  {
    sizeY = 25;
    active = false;
    colour = color(64);
  }

  public void display(float x, float y)
  {
    display(x, y, 255, false);
  }

  public void display(float x, float y, int alpha)
  {
    display(x, y, alpha, false);
  }

  public void display(float x, float y, int alpha, boolean highlighted)
  {
    display(g, x, y, alpha, highlighted);
  }

  public void display(PGraphics pg, float x, float y, int alpha, boolean highlighted)
  {
    int r = 255; 
    int g = 255; 
    int b = 255;
    //pg.beginDraw();
    //Highlight selected frames:
    if (active) 
    {
      r =  PApplet.parseInt(sin(PApplet.parseFloat(frameCount)/5)*127+128);
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

  public void toggleActive()
  {
    active = !active;
  }

  public void setInternalOffset(float offset)
  {
    yOffset = offset;
  }

  public void updateOffset()
  {
    yOffset += yOffVel;
    if (yOffVel < 0.25f) yOffVel = 0;
  }

  public void resetOffset()
  {
    if (yOffset!=0) yOffVel = -yOffset*0.25f;
  }

  public void addInternalOffset(float offset)
  {
    yOffset += offset;
  }

  public void reachValue(float offset)
  {
    if (yOffset != offset) yOffVel = (offset - yOffset)*0.25f;
  }

  public void setColour(int _colour)
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

  public void display(float x, float y, boolean graphical)
  {
    display(x, y, 255, false, graphical);
  }

  public void display(float x, float y, int alpha, boolean highlighted, boolean graphical)
  {
    display(g, x, y, alpha, highlighted, graphical);
  }

  public void display(PGraphics pg, float x, float y, int alpha, boolean highlighted, boolean graphical)
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
    if (laserboyMode) pg.fill(color(PApplet.parseInt((sin(0.5f*y/height+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(y/height+PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255), PApplet.parseInt((sin(0.5f*y/height+3*PI/2+PApplet.parseFloat(frameCount)/15)*0.5f+0.5f)*255)), alpha); 
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
    int visibleFrames = PApplet.parseInt((height-30)/frameSize);
    setTargetRect(20, 30, 120, visibleFrames*25-15);
  }

  public void draw() {
    if (overMain)
    {
      if (seqeditor != null) 
      {
        seqeditor.mainColour = color( PApplet.parseInt(sin(PApplet.parseFloat(frameCount)/5)*127+128), 50, 50);
        if (!seqeditor.shouldDisplayMain()) seqeditor.displayMain();
      }
    }
  }

  // if a dragged object enters the target area.
  // dropEnter is called.
  public void dropEnter() {
    overMain = true;
  }

  // if a dragged object leaves the target area.
  // dropLeave is called.
  public void dropLeave() {
    if (seqeditor != null) seqeditor.mainColour = color( 127);
    overMain = false;
  }

  public void dropEvent(DropEvent evt) {
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
    int visibleFrames = PApplet.parseInt((height-30)/frameSize);
    setTargetRect(160, 30, 120, visibleFrames*25-15);
  }

  public void draw() {
    if (overBuffer)
    {
      if (seqeditor != null) 
      {
        seqeditor.bufferColour = color( PApplet.parseInt(sin(PApplet.parseFloat(frameCount)/5)*127+128), 50, 50);
        if (!seqeditor.shouldDisplayBuffer()) seqeditor.displayBuffer();
      }
    }
  }

  // if a dragged object enters the target area.
  // dropEnter is called.
  public void dropEnter() {
    overBuffer = true;
  }

  // if a dragged object leaves the target area.
  // dropLeave is called.
  public void dropLeave() {
    if (seqeditor != null) seqeditor.bufferColour = color( 127);
    overBuffer = false;
  }

  public void dropEvent(DropEvent evt) {

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

  public void draw() {
    /*
    if (dragging)
     {
     draggable.display(mouseX, mouseY);
     }
     */
  }

  // if a dragged object enters the target area.
  // dropEnter is called.
  public void dropEnter() {
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
  public void dropLeave() {
    dragging = false;
  }

  public void dropEvent(DropEvent theEvent) {

    //Don't do anything
  }
}

  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "IldaViewer" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
