
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

  byte[] paletteToBytes()
  {

    ArrayList<Byte> theBytes;
    theBytes = new ArrayList<Byte>();

    theBytes.add(byte('I'));       //Bytes 1-4: "ILDA"
    theBytes.add(byte('L'));
    theBytes.add(byte('D'));
    theBytes.add(byte('A'));
    theBytes.add(byte(0));         //Bytes 5-8: Format Code 2
    theBytes.add(byte(0));
    theBytes.add(byte(0));
    theBytes.add(byte(2));



    for (int i = 0; i < 8; i++)    //Bytes 9-16: Name
    {
      char letter;
      if (name.length() < i+1) letter = ' ';
      else letter = name.charAt(i);
      theBytes.add(byte(letter));
    }



    if (companyName == null)   //Bytes 17-24: Company Name
    {
      theBytes.add(byte('I'));     //If empty: call it "IldaView"
      theBytes.add(byte('l'));
      theBytes.add(byte('d'));
      theBytes.add(byte('a'));
      theBytes.add(byte('V'));
      theBytes.add(byte('i'));
      theBytes.add(byte('e'));
      theBytes.add(byte('w'));
    }
    else
    {
      for (int i = 0; i < 8; i++)  
      {
        char letter;
        if (companyName.length() < i+1) letter = ' ';
        else letter = companyName.charAt(i);
        theBytes.add(byte(letter));
      }
    }

    int totalSize = colours.size();
    if (totalSize < 1) return null;
    if (totalSize > 255) totalSize = 256;

    theBytes.add(byte((totalSize>>8) & 0xff));              //Bytes 25-26: total colours 
    theBytes.add(byte(totalSize&0xff)); //Limited to 256 so byte 25 is redundant


    //Bytes 27-28: Palette number (just return activePalette)
    theBytes.add(byte((activePalette>>8) & 0xff));    //This better be correct
    theBytes.add(byte(activePalette & 0xff));

    theBytes.add(byte(0));    //Bytes 29-30: Future
    theBytes.add(byte(0));
    theBytes.add(byte(scannerHead)); //Byte 31: Scanner head
    theBytes.add(byte(0));    //Also Future



    for (int i = 0; i < min(256, colours.size()); i++)    //Rest: colour data
    {
      PaletteColour colour = colours.get(i);
      theBytes.add(byte(colour.getRed()));
      theBytes.add(byte(colour.getGreen()));
      theBytes.add(byte(colour.getBlue()));
    }

    byte [] bt = new byte[theBytes.size()];
    for (int i = 0; i<theBytes.size(); i++)
    {
      bt[i] = theBytes.get(i);
    }

    return bt;
  }

  void resizePalette(int newSize)
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

  void addColour(int r, int g, int b)
  {
    PaletteColour theColour = new PaletteColour(r, g, b);
    colours.add(theColour);
  }

  void addColour(color colour)
  {
    PaletteColour theColour = new PaletteColour(colour);
    colours.add(theColour);
  }

  PaletteColour getPaletteColour(int index)
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

  color getColour(int index)
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

  void formHeader()
  {
    hoofding.clear();
    hoofding.append("Frame: " + name);
    hoofding.append("Company: " + companyName);
    hoofding.append("Amount of colours: " + totalColors);
    hoofding.append("Palette number: " + paletteNumber);
    hoofding.append("Scanner head: " + scannerHead);
  }
}

