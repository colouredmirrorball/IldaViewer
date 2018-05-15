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
  void writeFile(Palette _palette)
  {
    writeFile(location, b, _palette);
  }

  void writeFile()
  {
    writeFile(location, b);
  }

  //Merge a palette table with an existing array of bytes (the palette is placed at the beginning of the file)
  void writeFile(String location, byte[] _b, Palette aPalette)
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
  void writeFile(String location, byte[] _b)
  {
    if (_b != null) saveBytes(location, _b);
  }

  //Convert an ArrayList of Frames to ilda-compliant bytes:
  byte[] framesToBytes(ArrayList<Frame> frames, int formatNumber)
  {
    if (frames.isEmpty()) return null;
    if (formatNumber == 0 || formatNumber == 1 || formatNumber == 4 || formatNumber == 5) return framesToIldaFile(frames, formatNumber);
    if (formatNumber == 6) return frameToPicFile(frames.get(0));
    if (formatNumber == 7) return framesToOldCatFile(frames);
    if (formatNumber == 8) return framesToCatFile(frames);
    return null;
  }

  byte[] framesToIldaFile(ArrayList<Frame> frames, int ildVersion)
  {
    ArrayList<Byte> theBytes;
    theBytes = new ArrayList<Byte>();
    int frameNum = 0;

    if (frames.isEmpty() ) return null;

    for (Frame frame : frames)
    {
      //println(frame.points.size());
      theBytes.add(byte('I'));
      theBytes.add(byte('L'));
      theBytes.add(byte('D'));
      theBytes.add(byte('A'));
      theBytes.add(byte(0));
      theBytes.add(byte(0));
      theBytes.add(byte(0));

      if (ildVersion == 0 || ildVersion == 1 || ildVersion == 2 || ildVersion == 4 || ildVersion == 5 ) theBytes.add(byte(ildVersion));
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
        theBytes.add(byte(letter));
      }

      if (frame.companyName.length() == 0)   //Bytes 17-24: Company Name
      {
        theBytes.add(byte('I'));     //If empty: call it "IldaView"
        theBytes.add(byte('l'));
        theBytes.add(byte('d'));
        theBytes.add(byte('a'));
        theBytes.add(byte('V'));
        theBytes.add(byte('i'));
        theBytes.add(byte('e'));
        theBytes.add(byte('w'));
      } else
      {
        for (int i = 0; i < 8; i++)  
        {
          char letter;
          if (frame.companyName.length() < i+1) letter = ' ';
          else letter = frame.companyName.charAt(i);
          theBytes.add(byte(letter));
        }
      }

      //Bytes 25-26: Total point count
      theBytes.add(byte((frame.points.size()>>8)&0xff));    //This better be correct
      theBytes.add(byte(frame.points.size()&0xff));


      //Bytes 27-28: Frame number (automatically increment each frame)
      theBytes.add(byte((++frameNum>>8)&0xff));    //This better be correct
      theBytes.add(byte(frameNum&0xff));


      //Bytes 29-30: Number of frames
      theBytes.add(byte((frames.size()>>8)&0xff));    //This better be correct
      theBytes.add(byte(frames.size()&0xff));

      theBytes.add(byte(frame.scannerHead));    //Byte 31 is scanner head
      theBytes.add(byte(0));                    //Byte 32 is future

      // Ilda V0: 3D, palette
      if (ildVersion == 0)
      {
        for (Point point : frame.points)
        {
          int posx = int(constrain(map(point.position.x, 0, width, -32768, 32767), -32768, 32768));
          theBytes.add(byte((posx>>8)&0xff));   
          theBytes.add(byte(posx & 0xff));

          int posy = int(constrain(map(point.position.y, height, 0, -32768, 32767), -32768, 32768));
          theBytes.add(byte((posy>>8)&0xff));   
          theBytes.add(byte(posy & 0xff));

          int posz = int(constrain(map(point.position.z, 0, depth, -32768, 32767), -32768, 32768));    
          theBytes.add(byte((posz>>8)&0xff));   
          theBytes.add(byte(posz & 0xff));

          if (point.blanked)
          {
            theBytes.add(byte(unbinary("01000000")));
          } else
          {
            theBytes.add(byte(0));
          }
          theBytes.add(byte(point.paletteIndex));
        }
      }

      //Ilda V1: 2D, palettes
      if (ildVersion == 1)
      {
        for (Point point : frame.points)
        {
          int posx = int(constrain(map(point.position.x, 0, width, -32768, 32767), -32768, 32768));
          theBytes.add(byte((posx>>8)&0xff));   
          theBytes.add(byte(posx & 0xff));

          int posy = int(constrain(map(point.position.y, height, 0, -32768, 32767), -32768, 32768));
          theBytes.add(byte((posy>>8)&0xff));   
          theBytes.add(byte(posy & 0xff));

          if (point.blanked)
          {
            theBytes.add(byte(unbinary("01000000")));
          } else
          {
            theBytes.add(byte(0));
          }
          theBytes.add(byte(point.paletteIndex));
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
          int posx = int(constrain(map(point.position.x, 0, width, -32768, 32767), -32768, 32768));
          theBytes.add(byte((posx>>8)&0xff));   
          theBytes.add(byte(posx & 0xff));

          int posy = int(constrain(map(point.position.y, height, 0, -32768, 32767), -32768, 32768));
          theBytes.add(byte((posy>>8)&0xff));   
          theBytes.add(byte(posy & 0xff));

          int posz = int(constrain(map(point.position.z, 0, depth, -32768, 32767), -32768, 32768));    
          theBytes.add(byte((posz>>8)&0xff));   
          theBytes.add(byte(posz & 0xff));

          if (point.blanked) theBytes.add(byte(unbinary("01000000")));
          else theBytes.add(byte(0));

          color c = point.colour;
          if (point.blanked) c = color(0, 0, 0);  //some programs only use colour information to determine blanking

          int red = (c >> 16) & 0xFF;  // Faster way of getting red(argb)
          int green = int((c >> 8) & 0xFF);   // Faster way of getting green(argb)
          int blue = int(c & 0xFF);          // Faster way of getting blue(argb)

          theBytes.add(byte(blue));
          theBytes.add(byte(green));
          theBytes.add(byte(red));
        }
      }

      //Ilda V5: 2D, BGR
      if (ildVersion == 5)
      {
        for (Point point : frame.points)
        {
          int posx = int(constrain(map(point.position.x, 0, width, -32768, 32767), -32768, 32768));  
          theBytes.add(byte((posx>>8)&0xff));   
          theBytes.add(byte(posx & 0xff));

          int posy = int(constrain(map(point.position.y, height, 0, -32768, 32767), -32768, 32768));
          theBytes.add(byte((posy>>8)&0xff));   
          theBytes.add(byte(posy & 0xff));



          if (point.blanked) theBytes.add(byte(unbinary("01000000")));
          else theBytes.add(byte(0));

          color c = point.colour;
          if (point.blanked) c = color(0, 0, 0);    //some programs only use colour information to determine blanking

          int red = (c >> 16) & 0xFF;  // Faster way of getting red(argb)
          int green = int((c >> 8) & 0xFF);   // Faster way of getting green(argb)
          int blue = int(c & 0xFF);          // Faster way of getting blue(argb)

          theBytes.add(byte(blue));
          theBytes.add(byte(green));
          theBytes.add(byte(red));
        }
      }
    }

    //File should always end with a header

    theBytes.add(byte('I'));
    theBytes.add(byte('L'));
    theBytes.add(byte('D'));
    theBytes.add(byte('A'));
    theBytes.add(byte(0));
    theBytes.add(byte(0));
    theBytes.add(byte(0));
    theBytes.add(byte(ildVersion));

    theBytes.add(byte('L'));
    theBytes.add(byte('A'));
    theBytes.add(byte('S'));
    theBytes.add(byte('T'));
    theBytes.add(byte(' '));
    theBytes.add(byte('O'));
    theBytes.add(byte('N'));
    theBytes.add(byte('E'));

    theBytes.add(byte('I'));     
    theBytes.add(byte('l'));
    theBytes.add(byte('d'));
    theBytes.add(byte('a'));
    theBytes.add(byte('V'));
    theBytes.add(byte('i'));
    theBytes.add(byte('e'));
    theBytes.add(byte('w'));

    theBytes.add(byte(0));
    theBytes.add(byte(0));

    theBytes.add(byte(0));
    theBytes.add(byte(0));

    theBytes.add(byte(0));
    theBytes.add(byte(0));

    theBytes.add(byte(0));

    theBytes.add(byte(0));




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
      theHeader[i] = char(abs(b[i]));
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

  IntList getFramePositions()
  {
    IntList positions = new IntList();
    for (int j=0; j<b.length-6; j++)
    {
      if (char(b[j]) == 'I' && char(b[j+1]) == 'L' && char(b[j+2]) == 'D' && char(b[j+3]) == 'A' && b[j+4] == 0 && b[j+5] == 0 && b[j+5] == 0)
      {
        positions.append(j);
      }
    }
    return positions;
  }

  //Read the file between the indices offset and end

  Frame readFrame(int offset, int end)
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
      theHeader[i] = char(abs(b[i]));
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
      name[i] = char(abs(b[i+8+offset]));
    } 

    //Bytes 16-23: company name
    char[] company = new char[8];
    for (int i = 0; i < 8; i++) {
      company[i] = char(abs(b[i+16+offset]));
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
    if (int(b[7+offset]) != 0 && int(b[7+offset]) != 1 && int(b[7+offset]) != 2 && int(b[7+offset]) != 4 && int(b[7+offset]) != 5)
    {
      status.add("Unsupported file format: " + int(b[7+offset]));
      return null;
    }

    //Is this a palette or a frame? 2 = palette, rest = frame
    if ( int(b[7+offset]) == 2)
    {
      status.add("Palette included");
      Palette palette = new Palette();

      palette.name = new String(name);
      palette.companyName = new String(company);
      palette.totalColors = unsignedShortToInt(pointCountt);

      //Byte 30: scanner head.
      palette.scannerHead = int(b[30+offset]);

      palette.formHeader();

      // ILDA V2: Palette information

      for (int i = 32+offset; i<end; i+=3)
      {
        palette.addColour(int(b[i]), int(b[i+1]), int(b[i+2]));
      }


      palettes.add(palette);
      activePalette = palettes.size()-1;
      return null;
    } else
    {
      Frame frame = new Frame();  //Frame(this);      <- remains here as a symbol of how not to program

      //Byte 7 = Ilda file version
      frame.ildaVersion = int(b[7+offset]);

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
      frame.scannerHead = int(b[30+offset]);

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
            float X = float(unsignedShortToInt(x));
            byte[] y = new byte[2];
            y[0] = b[i+2];
            y[1] = b[i+3];
            float Y = float(unsignedShortToInt(y));
            byte[] z = new byte[2];
            z[0] = b[i+4];
            z[1] = b[i+5];
            float Z = float(unsignedShortToInt(z));

            X = map(X, -32768, 32767, 0, width);
            Y = map(Y, 32768, -32767, 0, height);
            Z = map(Z, 32768, -32768, 0, depth);

            String statusString = binary(b[i+6])+binary(b[i+7]);
            boolean bl = false;
            if (statusString.charAt(1) == '1') bl = true;

            Point point = new Point(X, Y, Z, int(b[i+7]), bl);
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
            float X = float(unsignedShortToInt(x));
            byte[] y = new byte[2];
            y[0] = b[i+2];
            y[1] = b[i+3];
            float Y = float(unsignedShortToInt(y));
            X = map(X, -32768, 32767, 0, width);
            Y = map(Y, 32768, -32767, 0, height);

            String statusString = binary(b[i+4])+binary(b[i+5]);
            boolean bl = false;
            if (statusString.charAt(1) == '1') bl = true;


            Point point = new Point(X, Y, depth/2, int(b[i+5]), bl);
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
            float X = float(unsignedShortToInt(x));
            byte[] y = new byte[2];
            y[0] = b[i+2];
            y[1] = b[i+3];
            float Y = float(unsignedShortToInt(y));
            byte[] z = new byte[2];
            z[0] = b[i+4];
            z[1] = b[i+5];
            float Z = float(unsignedShortToInt(z));

            X = map(X, -32768, 32767, 0, width);
            Y = map(Y, 32768, -32767, 0, height);
            Z = map(Z, -32768, 32767, 0, depth);

            String statusString = binary(b[i+6]);
            boolean bl = false;
            if (statusString.charAt(1) == '1') bl = true;



            Point point = new Point(X, Y, Z, abs(int(b[i+9])), abs(int(b[i+8])), abs(int(b[i+7])), bl);
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
            float X = float(unsignedShortToInt(x));
            byte[] y = new byte[2];
            y[0] = b[i+2];
            y[1] = b[i+3];
            float Y = float(unsignedShortToInt(y));

            X = map(X, -32768, 32767, 0, width);
            Y = map(Y, 32768, -32767, 0, height);

            String statusString = binary(b[i+4]);
            boolean bl = false;
            if (statusString.charAt(1) == '1') bl = true;

            Point point = new Point(X, Y, depth/2, abs(int(b[i+7])), abs(int(b[i+6])), abs(int(b[i+5])), bl);
            frame.points.add(point);
          }
        }
      }

      return frame;
    }
  }

  Frame convertPicToIldaFrame()
  {
    if (b.length > 0)
    {
      int begin = b[0] == 0 ? 15 : 14;
      return convertPicToIldaFrame(b, begin, b.length, b[0]);
    }
    return null;
  }

  Frame convertPicToIldaFrame(byte[] b, int offset, int end, int version)
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
        float X = float(unsignedShortToInt(x));
        byte[] y = new byte[2];
        y[1] = b[i+2];
        y[0] = b[i+3];
        float Y = float(unsignedShortToInt(y));
        byte[] z = new byte[2];
        z[1] = b[i+4];
        z[0] = b[i+5];
        float Z = float(unsignedShortToInt(z));

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
            point = new Point(X, Y, Z, int(b[i+8]), int(b[i+9]), int(b[i+10]), bl);
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

  ArrayList<Frame> convertCatToFrames()
  {
    return convertCatToFrames(b);
  }

  ArrayList<Frame> convertCatToFrames(byte[] b)
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

  byte[] frameToPicFile(Frame frame)
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

  byte[] createPicPoint(Point point, boolean rgb)
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

  byte[] framesToOldCatFile(ArrayList<Frame> frames)
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

  byte[] framesToCatFile(ArrayList<Frame> frames)
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

