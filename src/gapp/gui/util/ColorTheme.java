package gapp.gui.util;

import javafx.scene.paint.Color;

public class ColorTheme
{
    public final String name;
    public final Color color_primary;
    public final Color color_secondary;
    
    public ColorTheme(String name, Color color_primary, Color color_secondary)
    {
        this.name = name;
        this.color_primary = color_primary;
        this.color_secondary = color_secondary;
    }
}
