boolean about = false;

void about()
{
  about = true;
}

void exitAbout()
{
  about = false;
}

void displayAbout()
{
  fill(laserboyMode ? color(int((sin(float(frameCount)/15)*0.5+0.5)*255), int((sin(PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin(3*PI/2+float(frameCount)/15)*0.5+0.5)*255)) : textcolour);
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

  fill(laserboyMode ? color(int((sin(0.5+float(frameCount)/15)*0.5+0.5)*255), int((sin(1.0+PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin(0.5+3*PI/2+float(frameCount)/15)*0.5+0.5)*255)) : textcolour);
  text("A detailed guide to using this program is in the Readme file.", 10, 140);
  text("There are also instructions on how to compile IldaViewer in Windows, OSX and Linux.", 10, 160);

  fill(laserboyMode ? color(int((sin(1.0+float(frameCount)/15)*0.5+0.5)*255), int((sin(2.0+PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin(1.0+3*PI/2+float(frameCount)/15)*0.5+0.5)*255)) : textcolour);
  text("If after reading the README and seeing the startup hints you still have questions,", 10, 190);
  text("or if you found a bug or have a suggestion, do not hesitate to leave a reply.", 10, 210);

  if (laserboyMode)
  {
    fill(50, 75, 200);
    String link2 = "This ";
    text(link2, 10, 240);
    fill(color(int((sin(1.5+float(frameCount)/15)*0.5+0.5)*255), int((sin(2.5+PI/2+float(frameCount)/15)*0.5+0.5)*255), int((sin(1.5+3*PI/2+float(frameCount)/15)*0.5+0.5)*255)) );
    text("is why it looks like an unicorn barfed over your screen.", 10+textWidth(link2), 240);
    if (mouseClicked && mouseOver(10, 220, 10+textWidth(link2), 240))
    {
      link("http://laserboy.org/");
    }
  }
}

