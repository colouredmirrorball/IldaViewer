
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
void sequenceCreator()
{
  seqcreator = true;
  if (sequenceCreator == null) sequenceCreator = new SequenceCreator();
  oscillAbstract();
  if (creatorMode != 1) creatorMode = 1;
}

//Exit the Sequence Creator tab:
void exitSequenceCreator()
{
  seqcreator = false;
}

//Click the Import button:
void finishSeqCreator()
{
  if (sequenceCreator != null) frames.addAll(sequenceCreator.getFrames());
  exitSequenceCreator();

  cp5.getWindow().activateTab("default");
  enterDefaultTab();

  cp5.getController("finishSeqCreator").setMouseOver(false);
}

//Clicked the clear button:
void clearSeqCreatorFrames()
{
  if (sequenceCreator != null) sequenceCreator.clearFrames();
}

void showSQBlanking(boolean theBlank)
{
  showBlanking = theBlank;
}

void initiateSeqCreatorMode(int mode)
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

  void update()
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

  void update3D()
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

  void clearFrames()
  {
    if (creatorMode == 0)
    {
      for (Frame frame : deluxe.theFrames)
      {
        frame.points.clear();
      }
    }
  }

  ArrayList<Frame> getFrames()
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
  void mousePressed()
  {
    if (creatorMode == 1)
    {
      if (osc != null) osc.mousePressed();
    }
  }
}

//           === DELUXE PAINT ===

void deluxePaint()
{
  cam.setActive(false);
  sequenceCreator.deluxe = new DeluxePaint();
  cp5.getController("showSQBlanking").setVisible(true);
  cp5.getController("clearSeqCreatorFrames").setVisible(true);

  status.clear();
  status.add("Mode Deluxe Paint entered. Drag around with the mouse.");
}

void exitDeluxePaint()
{
  frameRate(60);

  if (frames.size() > 1) cp5.getGroup("framePlayer").setVisible(true);
  if (frames.size()>0)
  {
    cp5.getController("clearFrames").setVisible(true);
    cp5.getController("showBlanking").setVisible(true);
    cp5.getController("showBlanking").setValue(float(int(showBlanking)));
  } else
  {
    cp5.getController("clearFrames").setVisible(false);
    cp5.getController("showBlanking").setVisible(false);
    cp5.getController("showBlanking").setValue(float(int(showBlanking)));
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
  float redFreq = random(0.1, 2);
  float greenFreq = random(0.1, 2);
  float blueFreq = random(0.1, 2);

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
  void update()
  {
    if (!theFrames.isEmpty())
    {

      int currFrame = frameCount % sequenceCreator.numberOfFrames;
      if (mousePressed )
      {

        if (cp5.getWindow().getMouseOverList().isEmpty() && !firstpoint)
        {
          Frame theFrame = theFrames.get(currFrame);
          color colour = color(int((sin(float(theFrame.points.size())*redFreq/sequenceCreator.numberOfFrames+redPhase)*0.5+0.5)*255), int((sin(float(theFrame.points.size())*greenFreq/sequenceCreator.numberOfFrames+greenPhase)*0.5+0.5)*255), int((sin(float(theFrame.points.size())*blueFreq/sequenceCreator.numberOfFrames+bluePhase)*0.5+0.5)*255));
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

void oscillAbstract()
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

void exitOscillabstract()
{
  cp5.getController("emptyOscElements").setVisible(false);
  cp5.getController("loadElements").setVisible(false);
  cp5.getController("saveElements").setVisible(false);
}

void emptyOscElements()
{
  cp5.getController("emptyOscElements").setLabel("Confirm... (enter)");
  sequenceCreator.osc.resetWorkspace = true;
  status.clear();
  status.add("Confirm resetting by pressing Enter. This will remove all elements and connections.");
}

//Load workspace cascade:
void loadElements()
{
  thread("loadOscWorkspace");
}

void loadOscWorkspace()
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

void oscFileLoad(File selection)
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
              int index = int(test[1]);
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
void saveElements()
{
  thread("saveOscWorkspace");
}

void saveOscWorkspace()
{
  String pathname;
  pathname = ".osc";
  File theFile = new File(pathname);
  status.clear();
  status.add("Select where to save the workspace");
  selectOutput("Save the current workspace...", "oscFileSave", theFile);
}

void oscFileSave(File selection)
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

  void resetWorkspace()
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
  int getIndex()
  {
    return currentIndex++;
  }

  void update()
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

  void mousePressed()
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
  Oscillelement searchElement(int index)
  {
    for (Oscillelement element : elements)
    {
      if (element.index == index) return element;
    }
    return null;
  }

  //Same
  Oscillelement searchElement(float x, float y)
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
        if (q[0].equals("X") ) x = int(q[1]);
        if (q[0].equals("Y") ) y = int(q[1]);
        if (q[0].equals("SizeX") ) sizex = int(q[1]);
        if (q[0].equals("SizeY") ) sizex = int(q[1]);
        if (q[0].equals("Index") ) index = int(q[1]);
      }
    }
  }

  //Display this element
  void display(boolean hide)
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
  void update()
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
  void mouseUpdate()
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

  void setPosition(float inx, float iny)
  {
    x -= inx;
    y -= iny;
  }

  //When this node has a Connection, the Connection is going to call this method (if it has the right type). 
  //The Connection keeps track of the name of the node as a string, so check which input it corresponds to and apply value
  void nodeInput(String nodeName, Frame frame)
  {
  }

  //Same here but with floats
  void nodeInput(String nodeName, float[] input)
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
  float[] getFloatArrayValue(String outName)
  {
    float[] empty = {
      0
    };
    return empty;
  }

  //Same but for frames
  Frame getFrameValue(String outName)
  {
    return null;
  }

  //Handle GUI events here. Loop through all GuiElements and check for their boolean active.
  //If it's true, it has been clicked on.
  void guiActionDetected()
  {
  }

  //Return a String[] with all the parameters or this element. Be as complete as possible so the exact state can be restored accurately.
  String[] getElementAsString()
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
  void nodeActionDetected(Node node)
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
  void resetNode(Node node)
  {
  }

  //Allows dynamically resizing the element
  void reachSize(float newSizex, float newSizey)
  {
    sizex += (newSizex - sizex)*0.25;
    sizey += (newSizey - sizey)*0.25;
  }

  //Call this upon exiting the element
  void closeElement()
  {
    deleteOscillelement = true;
    deletingElementIndex = index;
  }

  //convenience
  Node getNode(String name)
  {
    for (Node node : nodes)
    {
      if (node.name.equals(name)) return node;
    }
    return null;
  }

  Node searchNode(float xin, float yin)
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

  void guiActionDetected()
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
        if (element.name.equals("XYZ 2 Rθφ"))
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

  void update()
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
        //gui.add( new GuiButton(sizex - 115, 255, 90, 20, "XYZ 2 Rθφ"));
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

  void display()
  {
    super.display(sequenceCreator.osc.hideElements);
  }

  void newSource()
  {
    sequenceCreator.osc.elements.add(new Oscisource(x, y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }

  void newOscillator()
  {
    sequenceCreator.osc.elements.add(new Oscillator(x, y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }

  void newMerger()
  {
    sequenceCreator.osc.elements.add(new Oscimerger((int)x, (int) y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }

  void newBreakout()
  {
    sequenceCreator.osc.elements.add(new Oscibreakout((int)x, (int) y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  void newBreakin()
  {
    sequenceCreator.osc.elements.add(new Oscibreakin((int)x, (int) y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  void newSegment()
  {
    sequenceCreator.osc.elements.add(new Oscisegment((int)x, (int) y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  void newClip()
  {
    sequenceCreator.osc.elements.add(new Osciclip((int)x, (int) y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  void newColour()
  {
    sequenceCreator.osc.elements.add(new Oscicolour((int)x, (int) y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  void newPalette()
  {
    sequenceCreator.osc.elements.add(new Oscipalette((int)x, (int) y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  void newPalettifier()
  {
    sequenceCreator.osc.elements.add(new Oscipalettifier((int)x, (int) y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  void newBuffershift()
  {
    sequenceCreator.osc.elements.add(new Oscibuffershift((int)x, (int) y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  void newTranslator()
  {
    sequenceCreator.osc.elements.add(new Oscitranslate((int) x, (int) y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  void newRotator()
  {
    sequenceCreator.osc.elements.add(new Oscirotate((int) x, (int) y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  void newScaler()
  {
    sequenceCreator.osc.elements.add(new Osciscale((int) x, (int) y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  void newConstant()
  {
    sequenceCreator.osc.elements.add(new Oscilloconstant(x, y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  void newAdder()
  {
    sequenceCreator.osc.elements.add(new Oscilladder(x, y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  void newMultiplier()
  {
    sequenceCreator.osc.elements.add(new Osciplier(x, y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  void newMath()
  {
    sequenceCreator.osc.elements.add(new Oscimath(x, y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  void newLogic()
  {
    sequenceCreator.osc.elements.add(new Oscilogic(x, y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  void newClock()
  {
    sequenceCreator.osc.elements.add(new Oscilloclock(x, y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  void newRGB2HSB()
  {
    sequenceCreator.osc.elements.add(new OsciRGB2HSB(x, y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  void newXYZ2RThetaPhi()
  {
    sequenceCreator.osc.elements.add(new OsciXYZ2RThetaPhi(x, y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  void newInspect()
  {
    sequenceCreator.osc.elements.add(new Oscinspect(x, y));
    sequenceCreator.osc.addNewElement = false;
    sequenceCreator.osc.showAddDialog = false;
  }
  void newOptimise()
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

  void update()
  {
    super.update();
    if (startFrame != null) 
    {

      opt.setFrame(startFrame);
      opt.optimise();
      optimisedFrame = opt.getFrame();
    }
  }

  void display(boolean hide)
  {
    super.display(hide);
    //if (!opt.finished) 
    {

      //opt.display();
      //checkMouseOver = false;
    } 

    if (hide) return;
  }



  void generateGui()
  {
    //gui.add(new GuiDropdown(5, 80, 90, 20, "Options", true));
    gui.add(new GuiClose(sizex-15, 0));
    gui.add(new GuiButton(sizex-60, 80, 55, 20, "Settings"));
  }

  void resetNode(Node node)
  {
    if (node.name.equals("Input")) startFrame = new Frame();
  }

  void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new InNode(10, 25, "Input", 0));
    nodes.add(new OutNode(sizex-10, 25, "Output", 0));
  }

  void nodeInput(String nodeName, Frame inputFrame)
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

  Frame getFrameValue(String outName)
  {
    if (outName.equals("Output"))
    {
      return optimisedFrame.clone();
    }
    return null;
  }

  void guiActionDetected()
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



  String[] getElementAsString()
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
          swapped = boolean(q[1]);
        }
      }
    }
  }

  void update()
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
            float f = r[i]/60.0 - Hi;
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

  void display(boolean hide)
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

  void generateGui()
  {
    gui.add(new GuiButton(sizex*0.5-15, 20, 30, 20, "<->"));
    gui.add(new GuiClose(sizex-15, 0));
  }

  void generateNodes()
  {
    nodes.add(new InNode(10, 55, "_1", 1));
    nodes.add(new InNode(10, 75, "_2", 1));
    nodes.add(new InNode(10, 95, "_3", 1));
    nodes.add(new OutNode(sizex-10, 55, "_4", 1));
    nodes.add(new OutNode(sizex-10, 75, "_5", 1));
    nodes.add(new OutNode(sizex-10, 95, "_6", 1));
  }

  void resetNode(Node node)
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

  void nodeInput(String nodeName, float[] input)
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

  float[] getFloatArrayValue(String outName)
  {
    if (outName.equals("_4")) return outh;
    if (outName.equals("_5")) return outs;
    if (outName.equals("_6")) return outb;
    return null;
  }

  void guiActionDetected()
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

  String[] getElementAsString()
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
          origin = boolean(q[1]);
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
          swapped = boolean(q[1]);
        }
      }
    }
  }

  void update()
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
          float theta = (y[i]-0.5)*PI;
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

  void display(boolean hide)
  {
    super.display(hide);
    if (hide) return;
    stroke(0);
    textFont(f10);
    textAlign(LEFT);
    if (!swapped)
    {
      text("XYZ", x+5, y+35);
      text("Rθφ", x+sizex-25, y+35);
      text("X", x+20, y+58);
      text("Y", x+20, y+78);
      text("Z", x+20, y+98);
      text("R", x+sizex-25, y+58);
      text("θ", x+sizex-25, y+78);
      text("φ", x+sizex-25, y+98);
    } else
    {
      text("Rθφ", x+5, y+35);
      text("XYZ", x+sizex-25, y+35);
      text("R", x+20, y+58);
      text("θ", x+20, y+78);
      text("φ", x+20, y+98);
      text("X", x+sizex-25, y+58);
      text("Y", x+sizex-25, y+78);
      text("Z", x+sizex-25, y+98);
    }
  }

  void nodeInput(String nodeName, float[] input)
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

  void resetNode(Node node)
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

  float[] getFloatArrayValue(String outName)
  {
    if (outName.equals("_4")) return outr;
    if (outName.equals("_5")) return outt;
    if (outName.equals("_6")) return outp;
    return null;
  }

  void generateNodes()
  {
    nodes.add(new InNode(10, 55, "_1", 1));
    nodes.add(new InNode(10, 75, "_2", 1));
    nodes.add(new InNode(10, 95, "_3", 1));
    nodes.add(new OutNode(sizex-10, 55, "_4", 1));
    nodes.add(new OutNode(sizex-10, 75, "_5", 1));
    nodes.add(new OutNode(sizex-10, 95, "_6", 1));
  }

  void generateGui()
  {
    gui.add(new GuiButton(sizex*0.5-15, 20, 30, 20, "<->"));
    gui.add(new GuiButton(10, sizey-25, 50, 20, "Origin"));
    gui.add(new GuiClose(sizex-15, 0));
  }

  void guiActionDetected()
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

  String[] getElementAsString()
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

  void update()
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

  void generateGui()
  {
    gui.add(new GuiClose(sizex-15, 0));
  }

  void generateNodes()
  {
    nodes.add(new InNode(10, 25, "Input", 1));
    nodes.add(new InNode(10, 45, "Offset", 1));
    nodes.add(new OutNode(sizex-10, 25, "Output", 1));
  }

  void nodeInput(String nodeName, float[] input)
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

  void resetNode(Node node)
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

  float[] getFloatArrayValue(String outName)
  {
    if (outName.equals("Output")) return output;
    return null;
  }

  void guiActionDetected()
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
          details = boolean(q[1]);
        }
      }
    }
  }

  void update()
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

  void display(boolean hide)
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
      if (laserboyMode) stroke(color(int((sin(float(frameCount)/15)*0.5+0.5)*255), int((sin(PI/2+float(frameCount)/10)*0.5+0.5)*255), int((sin(3*PI/2+float(frameCount)/20)*0.5+0.5)*255))); 
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

  void generateGui()
  {
    gui.add(new GuiClose(sizex-15, 0));
    gui.add(new GuiToggle(sizex-45, 25, 40, 15, "Details"));
  }

  void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new InNode(10, 25, "Input_fl", 1));
    nodes.add(new InNode(10, 45, "Input_fr", 0));
  }

  void nodeInput(String nodeName, Frame inputFrame)
  {
    if (nodeName.equals("Input_fr"))
    {
      if (inputFrame != null)
      {
        frame = new Frame(inputFrame);
      }
    }
  }

  void nodeInput(String nodeName, float[] input)
  {
    if (nodeName.equals("Input_fl")) 
    {
      values = input;
    }
  }

  void guiActionDetected()
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

  void toggleDetails(float value)
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

  String[] getElementAsString()
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
      oi.frame.drawFrame(g, 5.0, 5.0, 0.0, s, s, s);
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

  void update()
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

  void generateGui()
  {
    gui.add(new GuiClose(sizex-15, 0));
  }

  void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new InNode(10, 25, "Input", 0));
    nodes.add(new InNode(10, 50, "X", 1));
    nodes.add(new InNode(10, 70, "Y", 1));
    nodes.add(new InNode(10, 90, "Z", 1));
    nodes.add(new OutNode(sizex-10, 25, "Output", 0));
  }

  //Gets called by the connection
  void nodeInput(String nodeName, Frame inputFrame)
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

  void nodeInput(String nodeName, float[] input)
  {
    if (nodeName.equals("X")) tx = input;
    if (nodeName.equals("Y")) ty = input;
    if (nodeName.equals("Z")) tz = input;
  }

  Frame getFrameValue(String outName)
  {
    if (outName.equals("Output"))
    {
      return translatedFrame.clone();
    }
    return null;
  }

  void guiActionDetected()
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

  void resetNode(Node node)
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

  void toggleOptions(float displayThem)
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
          pivot = boolean(q[1]);
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

  void update()
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

        float xind = point.position.x - (aanx[i]+0.5)*width;
        float yind = point.position.y - (aany[i]+0.5)*height;
        float zind = point.position.z - (aanz[i]+0.5)*depth;



        // x' = Rx (matrix multiplication)
        float xnew = R[0][0]*xind + R[0][1]*yind + R[0][2]*zind;
        float ynew = R[1][0]*xind + R[1][1]*yind + R[1][2]*zind;
        float znew = R[2][0]*xind + R[2][1]*yind + R[2][2]*zind;

        //Add the anchor point:
        point.position.x = xnew + (aanx[i]+0.5)*width;
        point.position.y = ynew + (aany[i]+0.5)*height;
        point.position.z = znew + (aanz[i]+0.5)*depth;
        rotatedFrame.points.add(point);
      }
    }
  }



  void generateGui()
  {
    //gui.add(new GuiDropdown(5, 80, 90, 20, "Options", true));
    gui.add(new GuiClose(sizex-15, 0));
    gui.add(new GuiButton(sizex-60, 80, 55, 20, "Pivot point"));
  }

  void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new InNode(10, 25, "Input", 0));
    nodes.add(new InNode(10, 50, "X", 1));
    nodes.add(new InNode(10, 70, "Y", 1));
    nodes.add(new InNode(10, 90, "Z", 1));
    nodes.add(new OutNode(sizex-10, 25, "Output", 0));
  }

  void nodeInput(String nodeName, Frame inputFrame)
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

  void nodeInput(String nodeName, float[] input)
  {
    if (nodeName.equals("X")) rx = input;
    if (nodeName.equals("Y")) ry = input;
    if (nodeName.equals("Z")) rz = input;
    if (nodeName.equals("Pivot X")) anx = input;
    if (nodeName.equals("Pivot Y")) any = input;
    if (nodeName.equals("Pivot Z")) anz = input;
  }

  Frame getFrameValue(String outName)
  {
    if (outName.equals("Output"))
    {
      return rotatedFrame.clone();
    }
    return null;
  }

  void guiActionDetected()
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

  void resetNode(Node node)
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
          anchor = boolean(q[1]);
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

  void update()
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

        float xind = point.position.x - (aanx[i]+0.5)*width;
        float yind = point.position.y - (aany[i]+0.5)*height;
        float zind = point.position.z - (aanz[i]+0.5)*depth;

        xind = ssx[i]*xind*whole[i];
        yind = ssy[i]*yind*whole[i];
        zind = ssz[i]*zind*whole[i];

        point.position.x = xind + (aanx[i]+0.5)*width;
        point.position.y = yind + (aany[i]+0.5)*height;
        point.position.z = zind + (aanz[i]+0.5)*depth;
        scaledFrame.points.add(point);
      }
    }
  }

  void generateGui()
  {
    //gui.add(new GuiDropdown(5, 80, 90, 20, "Options", true));
    gui.add(new GuiClose(sizex-15, 0));
    gui.add(new GuiButton(sizex-60, 100, 55, 20, "Anchor point"));
  }

  void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new InNode(10, 25, "Input", 0));
    nodes.add(new InNode(10, 50, "Combined", 1));
    nodes.add(new InNode(10, 70, "X", 1));
    nodes.add(new InNode(10, 90, "Y", 1));
    nodes.add(new InNode(10, 110, "Z", 1));
    nodes.add(new OutNode(sizex-10, 25, "Output", 0));
  }

  void nodeInput(String nodeName, Frame inputFrame)
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

  void nodeInput(String nodeName, float[] input)
  {
    if (nodeName.equals("Combined")) whole = input;
    if (nodeName.equals("X")) sx = input;
    if (nodeName.equals("Y")) sy = input;
    if (nodeName.equals("Z")) sz = input;
    if (nodeName.equals("Anchor X")) anx = input;
    if (nodeName.equals("Anchor Y")) any = input;
    if (nodeName.equals("Anchor Z")) anz = input;
  }

  Frame getFrameValue(String outName)
  {
    if (outName.equals("Output"))
    {
      return scaledFrame.clone();
    }
    return null;
  }

  void guiActionDetected()
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

  void resetNode(Node node)
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

  String[] getElementAsString()
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
    -0.5
  };
  float[] x2 = {
    0.5
  };
  float[] y1 = {
    -0.5
  };
  float[] y2 = {
    0.5
  };
  float[] z1 = {
    -0.5
  };
  float[] z2 = {
    0.5
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
          anchor = boolean(q[1]);
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

  boolean insideBox(PVector position, float left, float right, float bottom, float top, float front, float back)
  {
    return position.x >= left && position.x <= right && position.y <= bottom && position.y >= top && position.z >= front && position.z <= back;
  }

  byte getRegion(PVector position, float left, float right, float bottom, float top, float front, float back)
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

  void update()
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
      float[] xx1 = mapArray(l, x1, -0.5);
      float[] xx2 = mapArray(l, x2, 0.5);
      float[] yy1 = mapArray(l, y1, -0.5);
      float[] yy2 = mapArray(l, y2, 0.5);
      float[] zz1 = mapArray(l, z1, -0.5);
      float[] zz2 = mapArray(l, z2, 0.5);
      float[] aanx = mapArray(l, anx, 0);
      float[] aany = mapArray(l, any, 0);
      float[] aanz = mapArray(l, anz, 0);

      Point prevPoint = new Point(startFrame.points.get(startFrame.points.size()-1));
      boolean prevclip = false;

      for (int i = l-1; i >= 0; i--)
      {
        Point point = new Point(startFrame.points.get(i)); 
        int lbound = (int) ((xx1[i]-aanx[i]+0.5)*width);
        int rbound = (int) ((xx2[i]-aanx[i]+0.5)*width);
        int tbound = (int) ((yy1[i]-aany[i]+0.5)*height);
        int bbound = (int) ((yy2[i]-aany[i]+0.5)*height);
        int fbound = (int) ((zz1[i]-aanz[i]+0.5)*depth);
        int bkbound = (int) ((zz2[i]-aanz[i]+0.5)*depth);
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



  boolean shouldClip(float in1, float in2, int bound)
  {
    return (in1 < bound && in2 >= bound) || (in1 > bound && in2 <= bound);
  }

  float getClipCoord(float c1, float c2, float c3, float c4, float clipc)//example: x1, x2, y1, y2, xbound
  {
    return -(c3-c4)*(c1-clipc)/(c1-c2)+c3;
  }

  void generateGui()
  {
    //gui.add(new GuiDropdown(5, 80, 90, 20, "Options", true));
    gui.add(new GuiClose(sizex-15, 0));
    gui.add(new GuiButton(sizex-50, 140, 45, 20, "Anchor"));
  }

  void generateNodes()
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

  void nodeInput(String nodeName, Frame inputFrame)
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

  void nodeInput(String nodeName, float[] input)
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

  Frame getFrameValue(String outName)
  {
    if (outName.equals("Output"))
    {
      return clippedFrame.clone();
    }
    return null;
  }

  void guiActionDetected()
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

  void resetNode(Node node)
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

  String[] getElementAsString()
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

  void update()
  {
    super.update();
  }

  void generateGui()
  {
    //gui.add(new GuiDropdown(5, 80, 90, 20, "Options", true));
    gui.add(new GuiClose(sizex-15, 0));
  }

  void resetNode(Node node)
  {
    if (node.name.equals("Input")) startFrame = new Frame();
  }

  void generateNodes()
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

  void nodeInput(String nodeName, Frame inputFrame)
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

  float[] getFloatArrayValue(String outName)
  {
    if (outName.equals("X"))
    {
      float[] out = new float[startFrame.points.size()];
      for (int i = 0; i < startFrame.points.size (); i++)
      {
        out[i] = startFrame.points.get(i).position.x/width-0.5;
      }
      return out;
    }
    if (outName.equals("Y"))
    {
      float[] out = new float[startFrame.points.size()];
      for (int i = 0; i < startFrame.points.size (); i++)
      {
        out[i] = startFrame.points.get(i).position.y/height-0.5;
      }
      return out;
    }
    if (outName.equals("Z"))
    {
      float[] out = new float[startFrame.points.size()];
      for (int i = 0; i < startFrame.points.size (); i++)
      {
        out[i] = startFrame.points.get(i).position.z/depth-0.5;
      }
      return out;
    }
    if (outName.equals("R"))
    {
      float[] out = new float[startFrame.points.size()];
      for (int i = 0; i < startFrame.points.size (); i++)
      {
        out[i] = float((startFrame.points.get(i).colour >> 16) & 0xFF)/255;
      }
      return out;
    }
    if (outName.equals("G"))
    {
      float[] out = new float[startFrame.points.size()];
      for (int i = 0; i < startFrame.points.size (); i++)
      {
        out[i] = float((startFrame.points.get(i).colour >> 8) & 0xFF)/255;
      }
      return out;
    }
    if (outName.equals("B"))
    {
      float[] out = new float[startFrame.points.size()];
      for (int i = 0; i < startFrame.points.size (); i++)
      {
        out[i] = float(startFrame.points.get(i).colour & 0xFF)/255;
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
        out[i] = (float) int(startFrame.points.get(i).blanked);
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

  void guiActionDetected()
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
          numberOfPoints = int(q[1]);

          for (int i = 1; i <= numberOfPoints; i++)
          {
            outFrame.points.add(new Point(width*0.5, height*0.5, depth*0.5, 0, 0, 0, true));
          }
        }
      }
    }
  }

  void update()
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
      Point point = new Point((x[i]+0.5)*width, (y[i]+0.5)*height, (z[i]+0.5)*depth, (int) (r[i]*255), (int) (g[i]*255), (int) (b[i]*255), boolean((int) bl[i]));
      point.paletteIndex = (int) pI[i];
      outFrame.points.add(point );
    }
    outFrame.frameName = "Breakin";
    outFrame.companyName = "Oscillabstract";
    outFrame.pointCount = outFrame.points.size();

    nOPconnected = false;
  }

  void generateGui()
  {
    //gui.add(new GuiDropdown(5, 80, 90, 20, "Options", true));
    gui.add(new GuiClose(sizex-15, 0));
  }

  void generateNodes()
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

  void resetNode(Node node)
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

  void nodeInput(String nodeName, float[] input)
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

  Frame getFrameValue(String outName)
  {
    if (outName.equals("Output")) return outFrame;
    return null;
  }

  void guiActionDetected()
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

  void update()
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

  void generateGui()
  {
    //gui.add(new GuiDropdown(5, 80, 90, 20, "Options", true));
    gui.add(new GuiClose(sizex-15, 0));
  }

  void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new InNode(10, 25, "Input", 0));
    nodes.add(new OutNode(sizex-10, 45, "Segments", 1));
    nodes.add(new OutNode(sizex-10, 25, "Total", 1));
  }

  void resetNode(Node node)
  {
    if (node.name.equals("Input")) startFrame = new Frame();
  }

  void nodeInput(String nodeName, Frame inputFrame)
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

  float[] getFloatArrayValue(String outName)
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

  void guiActionDetected()
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
          multiply = boolean(q[1]);
        }
      }
    }
  }

  void update()
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
        color nlg = outFrame.points.get(i).colour;
        nlg = color(   max(0, min(((nlg >> 16) & 0xFF) * r[i]*ity[i], 255)), max(0, min(((nlg >> 8) & 0xFF) * g[i]*ity[i], 255)), max(0, min(((nlg  & 0xFF) * b[i]*ity[i]), 255)));
        outFrame.points.get(i).colour = nlg;
      } else
      {
        outFrame.points.get(i).colour = color(max(0, min((r[i]*ity[i])*255, 255)), max(0, min((g[i]*ity[i])*255, 255)), max(0, min((b[i]*ity[i])*255, 255)));
      }
    }
  }

  void generateGui()
  {
    gui.add(new GuiButton(sizex - 55, 70, 50, 20, "Overwrite"));
    gui.add(new GuiClose(sizex-15, 0));
  }

  void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new OutNode(sizex-10, 25, "Output", 0));
    nodes.add(new InNode(10, 25, "Input", 0));
    nodes.add(new InNode(10, 45, "R", 1));
    nodes.add(new InNode(10, 65, "G", 1));
    nodes.add(new InNode(10, 85, "B", 1));
    nodes.add(new InNode(10, 105, "I", 1));
  }

  void nodeInput(String nodeName, Frame input)
  {
    if (nodeName.equals("Input")) 
    {
      if (input != null) inFrame = new Frame(input.clone());
    }
  }

  void nodeInput(String nodeName, float[] input)
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

  void resetNode(Node node)
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

  Frame getFrameValue(String outName)
  {
    if (outName.equals("Output")) return outFrame;
    return null;
  }

  void guiActionDetected()
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

  String[] getElementAsString()
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
            toggleOptions(float(q[2]));
            if (float(q[2]) == 0) options = false;
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

  void update()
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

  void display(boolean hide)
  {
    super.display(hide);
    if (hide) return;

    if (options)
    {
      int i = 0;
      for (PaletteColour colour : palette.colours)
      {
        noStroke();

        colour.displayColour((int) x+10+5*(i%16), (int) y+110+5*int(i/16), 5);
        i++;
      }
      fill(0);
      textAlign(LEFT);
      textFont(f10);
      text(palette.name, x+10, y+127+5*(palette.colours.size()/16));
    }
  }

  void generateGui()
  {
    gui.add(new GuiDropdown(5, 50, 90, 20, "Options", true));
    gui.add(new GuiButton(5, 80, 40, 20, "<<"));
    gui.add(new GuiButton(55, 80, 40, 20, ">>"));
    gui.add(new GuiClose(sizex-15, 0));
  }

  void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new OutNode(sizex-10, 25, "Output", 0));
    nodes.add(new InNode(10, 25, "Input", 0));
    nodes.add(new InNode(10, 45, "Index", 1));
  }


  void nodeInput(String nodeName, Frame input)
  {
    if (nodeName.equals("Input")) 
    {
      if (input != null) inFrame = new Frame(input.clone());
    }
  }

  void nodeInput(String nodeName, float[] input)
  {

    if (nodeName.equals("Index"))
    {
      connected = true;
      if (input != null) ind = input;
    }
  }

  void resetNode(Node node)
  {

    if (node.name.equals("Index"))
    {
      ind = new float[0];
    }
    if (node.name.equals("Input")) inFrame = new Frame();
  }

  Frame getFrameValue(String outName)
  {
    if (outName.equals("Output")) return outFrame;
    return null;
  }

  void guiActionDetected()
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

  void toggleOptions(float displayThem)
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

  String[] getElementAsString()
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
            toggleOptions(float(q[2]));
            if (float(q[2]) == 0) options = false;
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

  void update()
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

  void display(boolean hide)
  {
    super.display(hide);
    if (hide) return;

    if (options)
    {
      int i = 0;
      for (PaletteColour colour : palette.colours)
      {
        noStroke();

        colour.displayColour((int) x+10+5*(i%16), (int) y+110+5*int(i/16), 5);
        i++;
      }
      fill(0);
      textAlign(LEFT);
      textFont(f10);
      text(palette.name, x+10, y+127+5*(palette.colours.size()/16));
    }
  }

  void generateGui()
  {
    gui.add(new GuiDropdown(5, 50, 90, 20, "Options", true));
    gui.add(new GuiButton(5, 80, 40, 20, "<<"));
    gui.add(new GuiButton(55, 80, 40, 20, ">>"));
    gui.add(new GuiClose(sizex-15, 0));
  }

  void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new OutNode(sizex-10, 25, "Output", 0));
    nodes.add(new InNode(10, 25, "Input", 0));
    nodes.add(new OutNode(sizex-10, 45, "Index", 1));
  }

  void nodeInput(String nodeName, Frame input)
  {
    if (nodeName.equals("Input")) 
    {
      if (input != null) inFrame = new Frame(input.clone());
    }
  }

  void resetNode(Node node)
  {
    if (node.name.equals("Input")) inFrame = new Frame();
  }

  Frame getFrameValue(String outName)
  {
    if (outName.equals("Output")) return outFrame;
    return null;
  }

  float[] getFloatArrayValue(String name)
  {
    if (name.equals("Index")) return ind;
    return null;
  }

  void guiActionDetected()
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

  void toggleOptions(float displayThem)
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

  String[] getElementAsString()
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
          numberOfInputs = int(q[1]);

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
            toggleOptions(float(q[2]));
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
                el.setValue(1-int(q[2]));
                guiActionDetected();
              }
            }
          }
        }
      }
    }
  }

  void generateGui()
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

  void update()
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

  void display(boolean hide)
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

  void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new InNode(10, 25, "Frame 1", 0));
    nodes.add(new InNode(10, 45, "Frame 2", 0));
    nodes.add(new OutNode(sizex-10, 25, "Output", 0));
  }

  void nodeInput(String nodeName, Frame inputFrame)
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

  Frame getFrameValue(String outName)
  {
    if (outName.equals("Output"))
    {
      return mergedFrame;
    }
    return null;
  }

  void guiActionDetected()
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

  void addInputNode()
  {
    nodes.add(new InNode(10, 45+(++numberOfInputs)*20, "Frame " + (numberOfInputs +2), 0));
    for (GuiElement element : gui)
    {
      if (!element.name.equals("close")) element.y += 20;
    }
    newYValue += 20;
  }

  void toggleOptions(float displayThem)
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

  void actX(float input)
  {
    mergOptions[2] = input;
  }

  void actY(float input)
  {
    mergOptions[4] = input;
  }

  void actZ(float input)
  {
    mergOptions[6] = input;
  }

  void actR(float input)
  {
    mergOptions[3] = input;
  }

  void actG(float input)
  {
    mergOptions[5] = input;
  }

  void actB(float input)
  {
    mergOptions[7] = input;
  }

  void actNumber(float input)
  {
    mergOptions[1] = input;
  }

  void actBlanking(float input)
  {
    mergOptions[8] = input;
  }

  void actPalette(float input)
  {
    mergOptions[9] = input;
  }

  void actNone()
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

  String[] getElementAsString()
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

  void generateNodes()
  {
    //RelX, RelY, name, type (0 = frame, 1 = float[], 2 = float)
    nodes.add(new InNode(10, 25, "Input", 0));
    nodes.add(new InNode(10, 45, "Sync", 1));
  }

  void display(boolean hide)
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

  void update()
  {
    super.update();
    if (millis() - lastTime > 1000/playbackSpeed && record)
    {
      activeFrame++;
      lastTime = millis();
    }
    sequenceCreator.osc.outFrames = outputFrames;
  }

  void nodeInput(String nodeName, Frame inputFrame)
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

  void nodeInput(String nodeName, float[] in)
  {
    if (nodeName.equals("Sync"))
    {
      if (in.length > 0) activeFrame = (int) in[0];
      if (activeFrame > maxFrames) maxFrames = activeFrame;
    }
  }

  void generateGui()
  {
    gui.add( new GuiToggle(sizex-displaySize-70, sizey-25, 55, 20, "Record"));
  }

  void guiActionDetected()
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
  int visibleFrames = int(width/frameSize);
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

        if (q[0].equals("Beginframe")) firstFrame = int(q[1]);
        if (q[0].equals("Endframe")) lastFrame = int(q[1]);
        if (q[0].equals("Programmedframe")) preprogFrame = int(q[1]);
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
            toggleOptions(1-float(q[2]));
          }
          if (q[1].equals("close"))
          {
          }
          if (q[1].equals("Autoplay"))
          { 
            toggleAutoplay(float(q[2]));
          }
        }
      }
    }
    if (preprogFrame >= 0 && preprogFrame < preprogframes.size())
    {
      sources.add(preprogframes.get(preprogFrame));
    }
  } 

  void display(boolean hide)
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
          frames.get(i).drawFrame((float)xoffset+frameSize*i, 160.0, 0.0, 90.0, 90.0, 0.0, false, true);
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
          preprogframes.get(i).drawFrame((float)xoffset2+frameSize*i, 300, 0.0, 90.0, 90.0, 0.0, false, true);
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
      rect(width*0.5- 55, height-180, 45, 20);
      rect(width*0.5- 5, height-180, 45, 20);

      fill(0);
      textFont(f12);
      text("Accept", width*0.5-35, height-167);
      text("Cancel", width*0.5+15, height-167);

      //Accept
      if (mousePressed && mouseX > width*0.5-55 && mouseX < width*0.5-10 && mouseY > height-180 && mouseY < height-160)
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
      if (mousePressed && mouseX > width*0.5-5 && mouseX < width*0.5+40 && mouseY > height-180 && mouseY < height-160)
      {
        selectTheFrames = false;
        checkMouseOver = true;
        sequenceCreator.osc.hideElements = false;
      }
    }
  }


  void update()
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

  Frame getFrameValue(String outName)
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

  float[] getFloatArrayValue(String outName)
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

  void guiActionDetected()
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

  void generateGui()
  {
    gui.add(new GuiDropdown(5, 145, 90, 20, "Options", false));
    gui.add(new GuiButton(5, 170, 90, 20, "Select frames"));
    gui.add(new GuiToggle(5, 195, 90, 20, "Autoplay"));
    gui.add(new GuiClose(sizex-15, 0));
  }

  void selectFrames()
  {
    mousePressed = false;
    selectTheFrames = true;
    sequenceCreator.osc.addNewElement = false;
    status.clear();
    status.add("Left click to select the first frame, right click to select the last frame.");
  }

  void toggleOptions(float displayThem)
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

  void toggleAutoplay(float shouldIt)
  {
    if (shouldIt == 1) 
    {
      autoplay = true;
    } else autoplay = false;
  }

  void resetNode(Node node)
  {
    if (node.name.equals("Sync_in"))
    {
      activeFrame = 0;
    }
  }

  void generatePreProgFrames(int numPoints)
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
      dotPoints.add(new Point(width*0.5, height*0.5, depth*0.5, 255, 255, 255, false));
    }
    dotFrame.points = dotPoints;
    dotFrame.frameName = "dot";
    preprogframes.add(dotFrame);

    //Line:
    Frame lineFrame = new Frame();
    ArrayList<Point> linePoints = new ArrayList<Point>();
    for (int i = 0; i <= numPoints; i++)
    {
      linePoints.add( new Point(float(i)*width/numPoints, height*0.5, depth*0.5, 255, 255, 255, false));
    }
    lineFrame.points = linePoints;
    lineFrame.frameName = "line";
    preprogframes.add(lineFrame);

    //Circle:
    Frame circleFrame = new Frame();
    ArrayList<Point> somePoints = new ArrayList<Point>();
    for (int i = 0; i <= numPoints; i++)
    {
      float x = map( sin(float(i)*TWO_PI/numPoints), -1, 1, 0, width);
      float y = map( cos(float(i)*TWO_PI/numPoints), -1, 1, 0, height);
      somePoints.add(new Point(x, y, depth*0.5, 255, 255, 255, false));
    }
    circleFrame.points = somePoints;
    circleFrame.frameName = "circle";
    preprogframes.add(circleFrame);

    //Triangle:
    Frame triFrame = new Frame();
    ArrayList<Point> triPoints = new ArrayList<Point>();
    for (int i = 0; i < numPoints*0.333; i++)
    {
      float x = map( i, 0, numPoints*0.333, 0, width);
      float y = height;
      triPoints.add(new Point(x, y, depth*0.5, 255, 255, 255, false));
    }
    for (int i = int (numPoints*0.333); i < int(numPoints*0.666); i++)
    {
      float x = map( i, numPoints*0.333, numPoints*0.666, width, width*0.5);
      float y = map( i, numPoints*0.333, numPoints*0.666, height, 0);
      triPoints.add(new Point(x, y, depth*0.5, 255, 255, 255, false));
    }
    for (int i = int (numPoints*0.666); i <= numPoints; i++)
    {
      float x = map( i, numPoints*0.666, numPoints, width*0.5, 0);
      float y = map( i, numPoints*0.666, numPoints, 0, height);
      triPoints.add(new Point(x, y, depth*0.5, 255, 255, 255, false));
    }
    triFrame.points = triPoints;
    triFrame.frameName = "triangle";
    preprogframes.add(triFrame);

    //Spiral:
    Frame spiralFrame = new Frame();
    ArrayList<Point> spiralPoints = new ArrayList<Point>();
    for (int i = 0; i <= numPoints; i++)
    {
      float x = map( float(i)/numPoints*sin(float(i)*2*TWO_PI/numPoints), -1, 1, 0, width);
      float y = map( float(i)/numPoints*cos(float(i)*2*TWO_PI/numPoints), -1, 1, 0, height);
      spiralPoints.add(new Point(x, y, i*depth/numPoints, 255, 255, 255, false));
    }
    spiralFrame.points = spiralPoints;
    spiralFrame.frameName = "spiral";
    preprogframes.add(spiralFrame);
  }

  void generateNodes()
  {
    nodes.add(new InNode(10, displaySize+45, "Sync_in", 1));
    nodes.add(new OutNode(sizex-10, displaySize+25, "Output", 0));
    nodes.add(new OutNode(sizex-10, displaySize+45, "Sync", 1));
  }

  void nodeInput(String nodeName, float[] input)
  {
    if (nodeName.equals("Sync_in"))
    {
      if (input.length > 0) activeFrame = (int) input[0];
    }
  }

  String[] getElementAsString()
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
        if (q[0].equals("Value")) value[0] = float(q[1]);
        if (q[0].equals("GUI") && q.length >= 3 )
        {

          if (q[1].equals("Value"))
          { 
            for (GuiElement el : gui)
            {
              if (el.name.equals("Value")) el.setValue(float(q[2]));
            }
          }
        }
      }
    }
  }

  void update()
  {
    super.update();
    //getNode("Output").setColour((int) min(255, max(0, value[0]))*255);
    for (GuiElement el : gui)
    {
      if (el.name.equals("Value")) value[0] = el.getValue();
    }

    getNode("Output").setColour(color((int) min(255, max(0, value[0]*255))));
  }

  void display(boolean hide)
  {
    super.display(hide);
  }

  void generateGui()
  {
    gui.add(new GuiClose(sizex-15, 0));
    gui.add(new GuiNumberBox(5, 45, 90, 20, "Value"));
  }

  void guiActionDetected()
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

  void generateNodes()
  {
    nodes.add(new OutNode(sizex-10, 25, "Output", 1));
  }

  void changeValue(float nr)
  {
    value[0] = nr;
    for (GuiElement el : gui)
    {
      if (el.name.equals("Value")) el.setValue(nr);
    }
  }

  float[] getFloatArrayValue(String outName)
  {
    if (outName.equals("Output")) return value;
    return null;
  }

  String[] getElementAsString()
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
        if (q[0].equals("Subtract")) subtract = boolean(q[1]);
        if (q[0].equals("Inputs")) numberOfInputs = int(q[1]);
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

  void update()
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

  void generateGui()
  {
    gui.add(new GuiClose(sizex-15, 0));
    gui.add(new GuiButton(sizex-50, 65, 45, 20, "Add"));
    gui.add(new GuiButton(5, 5+3*20, 20, 20, "+"));
  }

  void generateNodes()
  {
    nodes.add(new InNode(10, 25, "Value 1", 1));
    nodes.add(new InNode(10, 45, "Value 2", 1));
    nodes.add(new OutNode(sizex-10, 25, "Output", 1));
  }

  void nodeInput(String nodeName, float[] input)
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

  void resetNode(Node node)
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

  float[] getFloatArrayValue(String outName)
  {
    if (outName.equals("Output")) return output;
    return null;
  }

  void guiActionDetected()
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

  String[] getElementAsString()
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
        if (q[0].equals("Divide")) divide = boolean(q[1]);
        if (q[0].equals("Inputs")) numberOfInputs = int(q[1]);
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

  void update()
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
              else output[j] = 1E32;
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

  void generateGui()
  {
    gui.add(new GuiClose(sizex-15, 0));
    gui.add(new GuiButton(sizex-50, 65, 45, 20, "Multiply"));
    gui.add(new GuiButton(5, 5+(2+1)*20, 20, 20, "+"));
  }

  void generateNodes()
  {
    nodes.add(new InNode(10, 25, "Value 1", 1));
    nodes.add(new InNode(10, 45, "Value 2", 1));
    nodes.add(new OutNode(sizex-10, 25, "Output", 1));
  }

  void nodeInput(String nodeName, float[] input)
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

  void resetNode(Node node)
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

  float[] getFloatArrayValue(String outName)
  {
    if (outName.equals("Output")) return output;
    return null;
  }

  void guiActionDetected()
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

  String[] getElementAsString()
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
        if (q[0].equals("State")) state = int(q[1]);
      }
    }
    setFunctionNames();
    generateNodes();
    generateGui();   
    setFunction(state);
  }

  void display(boolean hide)
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

  void update()
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
        output[i] = pow(ina[i], 1.0/inb[i]);
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
        output[i] = ina[i] + (r < inb[i]*0.5 ? -r : inb[i]-r);
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
        output[i] = 0.5*(exp(ina[i])-exp(-ina[i]));
      }
      break;
    case 30 : 
      ina = mapArray(l, ina, 0);
      for (int i = 0; i < l; i++)
      {
        output[i] = 0.5*(exp(ina[i])+exp(-ina[i]));
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

  void setFunctionNames()
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

  void setFunction(int input)
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

  void generateGui()
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

  void generateNodes()
  {
    nodes.add(new InNode(10, 85, "a", 1));
    nodes.add(new InNode(10, 105, "b", 1));
    nodes.add(new InNode(10, 125, "c", 1));
    nodes.add(new InNode(10, 145, "d", 1));
    nodes.add(new OutNode(sizex-10, 85, "Output", 1));
  }

  void nodeInput(String nodeName, float[] input)
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

  void resetNode(Node node)
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

  float[] getFloatArrayValue(String outName)
  {
    if (outName.equals("Output")) return output;
    return null;
  }

  void guiActionDetected()
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

  String[] getElementAsString()
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
        if (q[0].equals("State")) state = int(q[1]);
        if (q[0].equals("Inputs")) addNodes = max(0, int(q[1])-3);
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

  void display(boolean hide)
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

    float textx = x + sizex*0.5;
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
      text("a ≤ {b, c, ... }", textx, texty, sizex-10, 45);
      break;
    case 9 : 
      text("a ≥ {b, c, ... }", textx, texty, sizex-10, 45);
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

  void update()
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

  void setFunctionNames()
  {
    formulaNames.append("If");
    formulaNames.append("Switch");
    formulaNames.append("And");
    formulaNames.append("Or");
    formulaNames.append("xOr");
    formulaNames.append("Not");
    formulaNames.append("<");
    formulaNames.append(">");
    formulaNames.append("≤");
    formulaNames.append("≥");
    formulaNames.append("=");
    formulaNames.append("≠");
    formulaNames.append("In range");
    formulaNames.append("Element of");
  }

  void setFunction(int input)
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

  void generateGui()
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

  void generateNodes()
  {
    addInNode();
    addInNode();
    addInNode();

    nodes.add(new OutNode(sizex-10, 85, "Output", 1));
  }

  void addInNode()
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

  String generateInputString(int input)
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

  int calculateInputString(String input)
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

  void nodeInput(String nodeName, float[] input)
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

  void resetNode(Node node)
  {
    int pos = calculateInputString(node.name);
    if (pos < inputs.size())
    {
      inputs.set(pos, new float[0]);
    }
    node.setColour(0);
  }

  float[] getFloatArrayValue(String outName)
  {
    if (outName.equals("Output")) return output;
    return null;
  }

  void guiActionDetected()
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

  String[] getElementAsString()
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
        if (q[0].equals("Count")) count = float(q[1]);
        if (q[0].equals("Frequency")) frequency = float(q[1]);
      }
    }
  }

  void update()
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

  void display(boolean hide)
  {
    super.display(hide);
  }

  void generateGui()
  {
    gui.add(new GuiClose(sizex-15, 0));
  }
  void guiActionDetected()
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

  void generateNodes()
  {
    nodes.add(new InNode(10, 45, "Frequency", 1));
    nodes.add(new InNode(10, 25, "Reset", 1));
    nodes.add(new InNode(10, 65, "Shape", 1));
    nodes.add(new OutNode(sizex-10, 25, "Output", 1));
  }

  void nodeInput(String nodeName, float[] input)
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

  void resetNode(Node node)
  {
    if (node.name.equals("Frequency")) frequency = 1;
    if (node.name.equals("Shape")) connected = false;
  }

  float[] getFloatArrayValue(String outName)
  {
    if (outName.equals("Output")) return output;
    return null;
  }

  String[] getElementAsString()
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
    0.5
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
        if (q[0].equals("Waveform")) waveform = float(q[1]);
        if (q[0].equals("Frequency")) 
        {
          frequency = new float[1];
          frequency[0] = float(q[1]);
        }
        if (q[0].equals("Amplitude")) 
        {
          amplitude =  new float[1];
          amplitude[0] =   float(q[1]);
        }
        if (q[0].equals("Phase")) 
        {
          phase =  new float[1];
          phase[0] =   float(q[1]);
        }
        if (q[0].equals("Offset")) 
        {
          offset =  new float[1];
          offset[0] =   float(q[1]);
        }
        if (q[0].equals("Samples")) samples = float(q[1]);

        if (q[0].equals("GUI") )
        {
        }
      }
    }
  }


  void update()
  {
    super.update();
    outvalues = oscillate(invalues);
    if (outvalues.length > 0) getNode("Output").setColour(color((int) min(255, max(0, outvalues[0]*255))));
    inputDisconnected = true;
  }

  void display(boolean hide)
  {
    super.display(hide);
    if (hide) return;
    fill(0);
    noStroke();
    rect(x+5, y+15, sizex-10, 40);
    if (outvalues.length > 0)
    {
      float prev = outvalues[0];
      if (laserboyMode) stroke(color(int((sin(float(frameCount)/15)*0.5+0.5)*255), int((sin(PI/2+float(frameCount)/10)*0.5+0.5)*255), int((sin(3*PI/2+float(frameCount)/20)*0.5+0.5)*255))); 
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

  float[] oscillate(float[] input)
  {
    if (samples == 0) samples = 1;
    int l  = (int) samples;
    if (l < 1) l = 1;

    if (inputDisconnected || input == null || input.length == 0)
    {
      input = new float[l];
      for (int i = 0; i < l; i++)
      {
        input[i] = float(i)/(l - ((waveform == 0 || waveform == 1) ? 1 : 0));
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
        random[i] = noise(l*0.03*i+prevrandom[i]*0.5);//, random[i]*l*0.01);
      }

      for (int i = 0; i <= l; i++)
      {
        output[i] = amplitude[i]*(random[i]-0.5)*2+offset[i];
      }
      break;
    default:
      break;
    }

    return output;
  }



  void generateGui()
  {
    gui.add(new GuiClose(sizex-15, 0));
  }

  void guiActionDetected()
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

  void generateNodes()
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

  void resetNode(Node node)
  {
    if (node.name.equals("Waveform")) waveform = 0;
    if (node.name.equals("Frequency")) frequency = new float[] { 
      1
    };
    if (node.name.equals("Phase")) phase = new float[] {
      0
    };
    if (node.name.equals("Amplitude")) amplitude = new float[] {
      0.5
    };
    if (node.name.equals("Offset"))offset = new float[] { 
      0
    };
    if (node.name.equals("Samples")) samples = 200;
  }

  void nodeInput(String nodeName, float[] input)
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



  float[] getFloatArrayValue(String outName)
  {
    if (outName.equals("Output")) return outvalues;
    return null;
  }

  String[] getElementAsString()
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

float tri(float in)
{
  in = in%1;
  if (in>=0.5 ) return 2*ramp(in)-1;
  if (in >= 0&& in < 0.5) return -2*ramp(in)-1;
  if (in < 0 && in > -0.5) return 2*ramp(in)-1;
  else return -2*ramp(in)-1;
}

float ramp(float in)
{
  if (in>=0) return 2*(in%1)-1;
  else
  {
    return 1-2*(abs(in)%1);
  }
}

float square(float in)
{
  if (in>=0)
  {
    if ((in%1)<0.5) return -1;
    else return 1;
  } else
  {
    if (abs(in%1)<0.5) return 1;
    else return -1;
  }
}

float sign(float in)
{
  if (in == 0) return 0;
  if (in < 0) return -1;
  return 1;
}

float[] mapArray(int newLength, float[] input, float defaultValue)
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

float roundToHalfInt(float input)
{
  float decimalPart = input%1;
  float integerPart = input - decimalPart;

  //Positive values:
  if (decimalPart < 0.25 && decimalPart >= 0) return integerPart;
  if (decimalPart >= 0.25 && decimalPart < 0.75) return integerPart + 0.5;
  if (decimalPart >= 0.75 && decimalPart < 1) return integerPart +1;

  //Negative values:
  if (decimalPart > -0.25 && decimalPart <= 0) return integerPart;
  if (decimalPart <= -0.25 && decimalPart > -0.75) return integerPart - 0.5;
  if (decimalPart <= -0.75 && decimalPart > -1) return integerPart -1;

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
  color colour = color(0);
  int type; //0 = Frame, 1 = float[], 2 = float

  Node(float x, float y, String name, int type)
  {
    this.x = x;
    this.y = y;
    this.name = name;
    this.type = type;
  }

  void update()
  {
  }

  void connect(int index)
  {
  }

  void setColour(color colour)
  {
    this.colour = colour;
  }

  void display(float elx, float ely)
  {
    if (laserboyMode) fill(color(int((sin((x)/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((y)+PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((elx+ely)/2+3*PI/2+float(frameCount)/15)*0.5+0.5)*255))); 
    else fill(colour);
    quad(x+elx-5, y+ely, x+elx, y+ely+5, x+elx+5, y+ely, x+elx, y+ely-5);
    textFont(f10);
  }

  boolean checkMouse(float elx, float ely)
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

  void display(float elx, float ely)
  {
    if (type == 0)
    {
      stroke(20, 100, 255);
      strokeWeight(1);
    } else noStroke();
    if (laserboyMode) fill(color(int((sin((x)/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((y)+PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((elx+ely)/2+3*PI/2+float(frameCount)/15)*0.5+0.5)*255))); 
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

  void display(float elx, float ely)
  {
    if (type == 0)
    {
      stroke(20, 100, 255);
      strokeWeight(1);
    } else noStroke();
    if (laserboyMode) fill(color(int((sin((x)/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((y)+PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin((elx+ely)/2+3*PI/2+float(frameCount)/15)*0.5+0.5)*255))); 
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
        if (q[0].equals("Type") ) type = int(q[1]);
        if (q[0].equals("InIndex") ) inIndex = int(q[1]);
        if (q[0].equals("OutIndex") ) outIndex = int(q[1]);
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

  void update()
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



  void display()
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
    bezier(x1, y1, x1+0.5*abs(x2-x1), y1, x2-0.5*abs(x2-x1), y2, x2, y2);
  }

  void dragInput(float x, float y)
  {
    active = true;
    x2 = x;
    y2 = y;
  }

  void connectInput(int inIndex, Node node)
  { 
    this.inIndex = inIndex;
    x2 = node.x;
    y2 = node.y;
    type = node.type;
    inName = node.name;
  }



  void dragOutput(float x, float y)
  {
    active = true;
    x1 = x;
    y1 = y;
  }

  void connectOutput(int outIndex, Node node)
  {
    this.inIndex = outIndex;
    x1 = node.x;
    y1 = node.y;
    type = node.type;
    outName = node.name;
  }


  void setCoordinates(float elx, float ely, Node node)  //Node = Output
  {
    if (node == null) return;
    x1 = elx + node.x;
    y1 = ely + node.y;
  }

  void setCoordinates( Node node, float elx, float ely)  //Node = Input
  {
    if (node == null) return;
    x2 = elx + node.x;
    y2 = ely + node.y;
  }

  void startDraggingInput()
  {
    shouldUpdate = false;
    dragInput = true;
  }

  void startDraggingOutput()
  {
    shouldUpdate = false;
    dragOutput = true;
  }

  boolean connectedToInput(int index, Node node)
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

  boolean connectedToOutput(int index, Node node)
  {
    if (outIndex == index && outName.equals(node.name) && node instanceof OutNode)
    {
      return true;
    } else return false;
  }

  String[] getConnectionAsString()
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
