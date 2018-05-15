// Fields for Palette Editor tab:

boolean paletteEditor = false;//Palette editor mode
ListBox paletteList;          //ControlP5 selection list 
PalEditor paleditor;
ColorPicker cp;               //ControlP5 colour picker


//      === PALETTE TAB METHODS AND CP5 CALLBACKS ===

// A beginMode() method should always get called upon entering a tab
void beginPalettes()
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
void exitPalettes()
{
  // The mode boolean should be set to false in here
  paletteEditor = false;
  status.clear();
  status.add("Palette editor exited");
}

void recolourFrames()
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

void paletteSelected(File selection) {
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
  int maxNum = int(theText);
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

  void update()
  {
    if (activePal >= 0  || activePal < palettes.size())
    {
      displayPalette(palettes.get(activePal));
    }
    fill(255);
    text(palettes.get(activePal).name, width-150, 460);
  }

  void updatePaletteList()
  {
    paletteList.clear();
    int i = 0;
    for (Palette palette : palettes)
    {
      paletteList.addItem(palette.name, i++);
    }
  }

  PImage getPaletteAsImage()
  {
    PImage pg = createImage(palettes.get(activePal).colours.size(), 1, RGB);
    for (int i = 0; i < palettes.get (activePal).colours.size(); i++)
    {
      pg.pixels[i] = palettes.get(activePal).colours.get(i).getColour();
    }
    return pg;
  }

  void importImageAsPalette(PImage img, String theName)
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
  void editPalette(int x, int y)
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

  void setActiveColour(color colour)
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


  void setActivePalette(int ind)
  {
    activePal = ind;
  }

  void displayPalette(Palette palette)
  {
    int i = 0;
    for (PaletteColour colour : palette.colours)
    {
      strokeWeight(0);
      if (i == activeCol) 
      {
        color c = color( int(sin(float(frameCount)/5)*127+128), 50, 50);
        stroke(c);
        strokeWeight(5);
      } else
      {
        stroke( color(50, 50, 50));
        strokeWeight(1);
      }
      colour.displayColour(width-425+25*(i%16), 25+25*int(i/16), 20);
      i++;
    }
  }
}

