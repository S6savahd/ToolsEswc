/**
 * AreaUtils.java
 *
 * Created on 13. 3. 2015, 17:01:42 by burgetr
 */
package org.fit.layout.eswc.op;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fit.layout.model.Area;
import org.fit.layout.model.Box;
import org.fit.layout.model.Rectangular;

/**
 * General purpose area analysis functions.
 * 
 * @author burgetr
 */
public class AreaUtils
{
    public static Set<String> blackShort;
    
    static {
        blackShort = new HashSet<String>();
        blackShort.add("IEEE");
        blackShort.add("ACM");
    }

    /**
     * Checks if the given areas are in the same visual group (i.e. "are near each other"). 
     * @param a1
     * @param a2
     * @return
     */
    public static boolean isNeighbor(Area a1, Area a2)
    {
        if (isOnSameLine(a1, a2))
            return true; //on the same line
        else
        {
            //the Y difference is less than half the line height
            int dy = a2.getBounds().getY1() - a1.getBounds().getY2();
            if (dy < 0)
                dy = a1.getBounds().getY1() - a2.getBounds().getY2();
            return dy < a1.getBounds().getHeight() / 2;
        }
    }
    
    /**
     * Checks if the given areas are on the same line.
     * TODO what if they don't share a parent?
     * @param a1
     * @param a2
     * @return
     */
    public static boolean isOnSameLine(Area a1, Area a2)
    {
        final int THRESHOLD = 1;
        final Rectangular gp1 = a1.getBounds();
        final Rectangular gp2 = a2.getBounds();
        return (Math.abs(gp1.getY1() - gp2.getY1()) <= THRESHOLD 
                && Math.abs(gp1.getY2() - gp2.getY2()) <= THRESHOLD); 
    }
    
    public static boolean isOnSameLineRoughly(Area a1, Area a2)
    {
        final Rectangular gp1 = a1.getBounds();
        final Rectangular gp2 = a2.getBounds();
        return (gp2.getY1() >= gp1.getY1() && gp2.getY1() < gp1.getY2())
                || (gp2.getY2() > gp1.getY1() && gp2.getY2() <= gp1.getY2());
    }
    
    /**
     * Checks if the given areas are in the same column.
     * @param a1
     * @param a2
     * @return
     */
    public static boolean isInSameColumn(Area a1, Area a2)
    {
        final Rectangular gp1 = a1.getTopology().getPosition();
        final Rectangular gp2 = a2.getTopology().getPosition();
        return (gp1.getX1() == gp2.getX1()); 
    }
    
    /**
     * Checks if the given areas are aligned in row or column.
     * @param a1
     * @param a2
     * @return
     */
    public static boolean isAligned(Area a1, Area a2)
    {
        final Rectangular gp1 = a1.getTopology().getPosition();
        final Rectangular gp2 = a2.getTopology().getPosition();
        return ((gp1.getX1() == gp2.getX1()) //x-aligned
                || (gp1.getY1() == gp2.getY1())); //y-aligned
    }
    
    /**
     * Checks if the given area has a target URL assigned (it acts as a link)
     * @param a
     * @return
     */
    public static boolean isLink(Area a)
    {
        for (Box box : a.getBoxes())
        {
            if (box.getAttribute("href") != null)
                return true;
        }
        return false;
    }

    /**
     * Finds short titles in the area using regexp.
     * @param a
     * @return
     */
    public static Vector<String> findShortTitles(Area a)
    {
        return findShortTitles(a.getText().trim());
    }
    
    public static Vector<String> findShortTitles(String text)
    {
        Vector<String> ret = new Vector<String>();
        Pattern pattern = Pattern.compile("[A-Z][A-Za-z0-9]*[A-Z]");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find())
        {
            final String sname = matcher.group(0);
            if (sname.length() >= 2 && sname.length() <= 6 && !blackShort.contains(sname))
                ret.add(sname);
        }
        return ret;
    }
}
