
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
    red = int(random(0, 255));
    green = int(random(0, 255));
    blue = int(random(0, 255));
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

  PaletteColour( color colour)
  {
    red = (colour >> 16) & 0xFF;  // Faster way of getting red(argb)
    green = (colour >> 8) & 0xFF;   // Faster way of getting green(argb)
    blue = colour & 0xFF;          // Faster way of getting blue(argb)
  }


  void changeColour(color colour)
  {
    //int a = (colour >> 24) & 0xFF;
    red = (colour >> 16) & 0xFF;  // Faster way of getting red(argb)
    green = (colour >> 8) & 0xFF;   // Faster way of getting green(argb)
    blue = colour & 0xFF;          // Faster way of getting blue(argb)
  }

  void displayColour(int x, int y, int size)
  {
    fill(color(red, green, blue));
    rect(x, y, size, size);
  }

  int getRed()
  {
    return red;
  }

  int getGreen()
  {
    return green;
  }

  int getBlue()
  {
    return blue;
  }

  int getYellow()
  {
    return yellow;
  }

  int getCyan()
  {
    return cyan;
  }

  int getDarkBlue()
  {
    return dblue;
  }

  color getColour()
  {
    return color(red, green, blue);
  }
}


