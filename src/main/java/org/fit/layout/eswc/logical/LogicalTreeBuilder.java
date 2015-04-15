/**
 * LogicalTreeBuilder.java
 *
 * Created on 19. 3. 2015, 13:54:48 by burgetr
 */
package org.fit.layout.eswc.logical;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.fit.layout.classify.taggers.DateTagger;
import org.fit.layout.eswc.Countries;
import org.fit.layout.eswc.CountriesTagger;
import org.fit.layout.eswc.op.AreaUtils;
import org.fit.layout.eswc.op.EswcTag;
import org.fit.layout.impl.BaseLogicalTreeProvider;
import org.fit.layout.model.Area;
import org.fit.layout.model.AreaTree;
import org.fit.layout.model.Box;
import org.fit.layout.model.LogicalArea;
import org.fit.layout.model.LogicalAreaTree;
import org.fit.layout.model.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO:
//- strip (ENDING) an trailing ., in editor affiliations (and conf titles?)
//- colocated may be also 'in conjunction with' -- vol-862
//  nebo 'located at' -- vol-540
//- napojeni editoru muze byt i (1) -- vol-862
//- lepsi regularni vyraz na zkratky -- vol-859
//- vol-895 chybi keynotes na konci
//- zkusit i indent pro hledani sekci -- vol-250
//-+ vol-53 zmatek v editorech, chybi id u paperu
//+ ALT atributy u obrazku -- vol-53
//- multi-workshop volumes
//- vol-225 Georgia, affiliations


/**
 *
 * @author burgetr
 */
public class LogicalTreeBuilder extends BaseLogicalTreeProvider
{
    private static Logger log = LoggerFactory.getLogger(LogicalTreeBuilder.class);
    
    private static final float ms = 0.5f;
    
    private static int paperIdCnt = 0; 
    
    private static Tag tagRoot = new EswcTag("root");
    private static Tag tagVTitle = new EswcTag("vtitle");
    private static Tag tagVShort = new EswcTag("vshort");
    private static Tag tagColoc = new EswcTag("colocated");
    private static Tag tagVCountry = new EswcTag("vcountry");
    private static Tag tagVDate = new EswcTag("vdate");
    private static Tag tagStartDate = new EswcTag("startdate");
    private static Tag tagEndDate = new EswcTag("enddate");
    private static Tag tagVEditor = new EswcTag("veditor");
    private static Tag tagEAffil = new EswcTag("eaffil");
    private static Tag tagECountry = new EswcTag("ecountry");
    private static Tag tagPaper = new EswcTag("paper");
    private static Tag tagAuthor = new EswcTag("authors");
    private static Tag tagTitle = new EswcTag("title");
    private static Tag tagPages = new EswcTag("pages");
    private static Tag tagStartPage = new EswcTag("pstart");
    private static Tag tagEndPage = new EswcTag("pend");
    private static Tag tagSection = new EswcTag("section");
    
    private EswcLogicalTree tree;
    private LogicalArea rootArea;
    private Vector<Area> leaves;
    private String shortname;
    
    private int iTitle = -1;
    private int iShort = -1;
    private int iColoc = -1;
    private int iDates = -1;
    private int iCountry = -1;
    private int editorStart = -1;
    private int editorEnd = -1;
    private int paperStart = -1;
    private int paperEnd = -1;

    @Override
    public String getId()
    {
        return "CEUR.Logical";
    }

    @Override
    public String getName()
    {
        return "CEUR Logical Tree Builder";
    }

    @Override
    public String getDescription()
    {
        return "Logical structure builder for CEUR proceedings";
    }

    @Override
    public String[] getParamNames()
    {
        return new String[0];
    }

    @Override
    public ValueType[] getParamTypes()
    {
        return new ValueType[0];
    }

    //====================================================================================
    
    private void reset()
    {
        iTitle = -1;
        iShort = -1;
        iColoc = -1;
        iDates = -1;
        iCountry = -1;
        editorStart = -1;
        editorEnd = -1;
        paperStart = -1;
        paperEnd = -1;
        shortname = null;
        paperIdCnt = 0;
    }
    
    @Override
    public LogicalAreaTree createLogicalTree(AreaTree areaTree)
    {
        reset();
        
        rootArea = createRoot(areaTree);
        tree = new EswcLogicalTree(areaTree);
        tree.setRoot(rootArea);
        
        leaves = new Vector<Area>();
        findLeaves(areaTree.getRoot(), leaves);
        
        scanLeaves();
        
        addVTitle();
        addVShort();
        addColoc();
        addVDates();
        addVCountry();
        addEditors();
        addPapers();
        
        return tree;
    }

    private void findLeaves(Area root, Vector<Area> dest)
    {
        if (root.isLeaf())
        {
            dest.add(root);
        }
        else
        {
            for (int i = 0; i < root.getChildCount(); i++)
                findLeaves(root.getChildArea(i), dest);
        }
    }
    
    private void scanLeaves()
    {
        int i = 0;
        for (Area a : leaves)
        {
            if (iTitle == -1 && a.hasTag(tagVTitle, ms))
                iTitle = i;
            if (iShort == -1 && a.hasTag(tagVShort, ms))
                iShort = i;
            if (iColoc == -1 && a.hasTag(tagColoc, ms))
                iColoc = i;
            if (iDates == -1 && a.hasTag(tagVDate, ms))
                iDates = i;
            if (iCountry == -1 && a.hasTag(tagVCountry, ms))
                iCountry = i;
            if (a.hasTag(tagVEditor, ms))
            {
                if (editorStart == -1) editorStart = i;
                editorEnd = i;
            }
            if (a.hasTag(tagTitle, ms) || a.hasTag(tagAuthor, ms) || a.hasTag(tagPages, ms))
            {
                if (paperStart == -1) paperStart = i;
                paperEnd = i;
            }
            
            i++;
        }
    }
    
    private LogicalArea createRoot(AreaTree tree)
    {
        String uri = tree.getRoot().getAllBoxes().firstElement().getPage().getSourceURL().toString();
        return new EswcLogicalArea(tree.getRoot(), uri, tagRoot);
    }
    
    //====================================================================================

    private void addVTitle()
    {
        Area root = leaves.elementAt(iTitle);
        String text = root.getText();
        rootArea.appendChild(new EswcLogicalArea(root, text, tagVTitle));
    }

    private void addVShort()
    {
        if (iShort != -1)
        {
            Area root = leaves.elementAt(iShort);
            Vector<String> snames = AreaUtils.findShortTitles(root);
            if (!snames.isEmpty())
            {
                shortname = snames.firstElement();
                rootArea.appendChild(new EswcLogicalArea(root, shortname, tagVShort));
            }
        }
    }
    
    private void addColoc()
    {
        int start = (iColoc == -1) ? iTitle + 1 : iColoc;
        int end = editorStart;
        if (iCountry != -1)
            end = iCountry;
        if (iDates != -1 && iDates < end)
            end = iDates;
        for (int i = start; i < end; i++)
        {
            Area a = leaves.elementAt(i);
            Vector<String> snames = AreaUtils.findShortTitles(a);
            for (String sn : snames)
            {
                if (shortname == null || !shortname.equals(sn))
                {
                    rootArea.appendChild(new EswcLogicalArea(a, sn, tagColoc));
                    return;
                }
            }
        }
    }
    
    private void addVDates()
    {
        if (iDates != -1)
        {
            Area a = leaves.elementAt(iDates);
            DateTagger dt = new DateTagger();
            List<Date> dates = dt.extractDates(a.getText());
            SimpleDateFormat dfmt = new SimpleDateFormat("yyyy-MM-dd");
            if (dates.size() == 1)
            {
                rootArea.appendChild(new EswcLogicalArea(a, dfmt.format(dates.get(0)), tagStartDate));
                rootArea.appendChild(new EswcLogicalArea(a, dfmt.format(dates.get(0)), tagEndDate));
            }
            else if (dates.size() == 2)
            {
                rootArea.appendChild(new EswcLogicalArea(a, dfmt.format(dates.get(0)), tagStartDate));
                rootArea.appendChild(new EswcLogicalArea(a, dfmt.format(dates.get(1)), tagEndDate));
            }
            else
                log.warn("Strange number of date values: {} in {}", dates, a.getText());
        }
        else
            log.warn("No dates found");
    }
    
    private void addVCountry()
    {
        if (iCountry != -1)
        {
            Area a = leaves.elementAt(iCountry);
            CountriesTagger ct = new CountriesTagger();
            List<String> countries = ct.extract(a.getText());
            if (countries.size() >= 1)
            {
                String uri = Countries.getCountryUri(countries.get(0));
                rootArea.appendChild(new EswcLogicalArea(a, uri, tagVCountry));
            }
            if (countries.size() != 1)
                log.warn("Strange number of countries: {} in {}", countries, a.getText());
        }
        else
            log.warn("No country found");
        
    }
    
    //====================================================================================
    
    private void addEditors()
    {
        //extend editors till the end of the last line
        Area last = leaves.elementAt(editorEnd);
        while (editorEnd + 1 < leaves.size())
        {
            Area next = leaves.elementAt(editorEnd + 1);
            if (AreaUtils.isOnSameLineRoughly(last, next))
            {
                last = next;
                editorEnd++;
            }
            else
                break;
        }
        //find editor data
        Area curEd = null;
        StringBuilder textb = null;
        for (int i = editorStart; i <= editorEnd; i++)
        {
            Area a = leaves.elementAt(i);
            if (a.hasTag(tagVEditor, ms))
            {
                if (curEd != null)
                    saveEditor(curEd, textb.toString().trim());
                curEd = a;
                textb = new StringBuilder(a.getText());
            }
            else
            {
                if (textb != null)
                    textb.append(a.getText());
            }
        }
        if (curEd != null)
            saveEditor(curEd, textb.toString().trim());
    }
    
    private void saveEditor(Area editor, String text)
    {
        //System.out.println("ED: " + text);
        //find the name
        int i = 0;
        while (i < text.length())
        {
            char ch = text.charAt(i);
            if (ch != '.' && ch != '-' 
                    && !Character.isLetter(ch) 
                    && !Character.isSpaceChar(ch))
                break;
            i++;
        }
        if (i > 0)
        {
            String name = text.substring(0, i);
            String affil = text.substring(i).trim();
            while (affil.startsWith(","))
                affil = affil.substring(1).trim();
            while (affil.endsWith(","))
                affil = affil.substring(0, affil.length() - 1);
            
            if (affil.isEmpty())
            {
                Area follow = leaves.elementAt(editorEnd+1);
                if (AreaUtils.isNeighbor(editor, follow))
                {
                    String ftext = getTextOnLine(editorEnd+1, paperStart);
                    affil = ftext.trim();
                }
                else
                    log.warn("No affiliation for " + text);
            }
            
            affil = completeAffil(affil);
            String affs[] = splitAffilCountry(affil);
            affil = affs[0];
                    
            LogicalArea aname = new EswcLogicalArea(editor, name, tagVEditor);
            rootArea.appendChild(aname);
            LogicalArea aaffil = new EswcLogicalArea(editor, affil, tagEAffil);
            aname.appendChild(aaffil);
            if (affs[1] != null)
            {
                LogicalArea acountry = new EswcLogicalArea(editor, affs[1], tagECountry);
                aname.appendChild(acountry);
            }
        }
        else
            log.warn("Couldn't find editor name: {}", text);
    }
    
    private String completeAffil(String src)
    {
        if (src.length() > 0 && !Character.isLetter(src.charAt(0)))
        {
            for (int i = editorEnd + 1; i < paperStart; i++)
            {
                final Area a = leaves.elementAt(i);
                final String s = a.getText();
                if (s.trim().startsWith(src))
                {
                    String ret = getTextOnLine(i, paperStart).trim();
                    ret = ret.substring(src.length()).trim();
                    return ret;
                }
            }
            log.warn("Couldn't find affiliation for " + src);
            return src;
        }
        else
            return src;
    }
    
    private String[] splitAffilCountry(String src)
    {
        String[] ret = new String[2];
        String affil = new String(src.trim());
        while (affil.endsWith(","))
            affil = affil.substring(0, affil.length() - 1).trim();
        int pos = affil.lastIndexOf(",");
        if (pos > 0)
        {
            String cname = affil.substring(pos + 1).trim().toLowerCase();
            String curi = Countries.getCountryUri(cname);
            if (curi != null)
            {
                ret[0] = affil.substring(0, pos).trim();
                ret[1] = curi;
            }
            else
            {
                ret[0] = affil;
                ret[1] = null;
                log.warn("No country found in '{}' ({}?)", affil, cname);
            }
        }
        else
        {
            ret[0] = affil;
            ret[1] = null;
            log.warn("No country position found in '{}'", affil);
        }
        
        return ret;
    }
    
    //====================================================================================
    
    private void addPapers()
    {
        Area curTitle = null;
        Area curAuthors = null;
        Area curPages = null;
        Area curSection = null;
        
        for (int i = paperStart; i <= paperEnd; i++)
        {
            Area a = leaves.elementAt(i);
            if (a.hasTag(tagTitle, ms) || a.hasTag(tagAuthor, ms) || a.hasTag(tagPages, ms))
            {
                if (curTitle == null && a.hasTag(tagTitle, ms))
                {
                    curTitle = a;
                    curSection = findSection(i);
                }
                else if (curAuthors == null && a.hasTag(tagAuthor, ms))
                    curAuthors = a;
                else if (curPages == null && a.hasTag(tagPages, ms))
                    curPages = a;
                else //some duplicate
                {
                    savePaper(curTitle, curAuthors, curPages, curSection);
                    curTitle = null;
                    curAuthors = null;
                    curPages = null;
                    i--; //try the same area again
                }
            }
        }
        savePaper(curTitle, curAuthors, curPages, curSection);
    }
    
    private Area findSection(int start)
    {
        float fsize = leaves.elementAt(start).getFontSize();
        for (int i = start - 1; i > editorEnd; i--)
        {
            Area a = leaves.elementAt(i);
            if (a.getFontSize() > fsize)
            {
                if (a.getText().equalsIgnoreCase("Table of Contents"))
                    return null;
                else
                    return a;
            }
        }
        return null;
    }
    
    private void savePaper(Area title, Area authors, Area pages, Area curSection)
    {
        if (title != null && authors != null)
        {
            String pid = findPaperId(title);
            if (pid != null)
            {
                LogicalArea ap = new EswcLogicalArea(title, pid, tagPaper);
                rootArea.appendChild(ap);
                
                LogicalArea at = new EswcLogicalArea(title, title.getText().trim(), tagTitle);
                ap.appendChild(at);
                String saut = authors.getText().trim();
                String names[] = saut.split("\\s*[,;]\\s*(and)?\\s*");
                for (String name : names)
                {
                    String nnames[] = name.split("\\s+and\\s+");
                    for (String nname : nnames)
                    {
                        LogicalArea aa = new EswcLogicalArea(authors, nname, tagAuthor);
                        ap.appendChild(aa);
                    }
                }
                if (pages != null)
                {
                    String[] pp = pages.getText().trim().split("\\s*\\p{Pd}\\s*");
                    if (pp.length == 2)
                    {
                        try {
                            int pi1 = Integer.parseInt(pp[0]);
                            int pi2 = Integer.parseInt(pp[1]);
                            LogicalArea ps = new EswcLogicalArea(pages, pp[0], tagStartPage);
                            ap.appendChild(ps);
                            LogicalArea pe = new EswcLogicalArea(pages, pp[1], tagEndPage);
                            ap.appendChild(pe);
                            LogicalArea pn = new EswcLogicalArea(pages, String.valueOf(pi2 - pi1 + 1), tagPages);
                            ap.appendChild(pn);
                        } catch (NumberFormatException e) {
                            log.warn("Invalid page numbers: {}", pages);
                        }
                    }
                    else if (pp.length == 1)
                    {
                        try {
                            Integer.parseInt(pp[0]);
                            LogicalArea ps = new EswcLogicalArea(pages, pp[0], tagStartPage);
                            ap.appendChild(ps);
                            LogicalArea pe = new EswcLogicalArea(pages, pp[0], tagEndPage);
                            ap.appendChild(pe);
                            LogicalArea pn = new EswcLogicalArea(pages, "1", tagPages);
                            ap.appendChild(pn);
                        } catch (NumberFormatException e) {
                            log.warn("Invalid page numbers: {}", pages);
                        }
                    }
                    else
                        log.warn("Invalid pages: {}", pages);
                }
                if (curSection != null)
                {
                    LogicalArea asec = new EswcLogicalArea(curSection, curSection.getText().trim(), tagSection);
                    ap.appendChild(asec);
                }
            }
            else
                log.error("Couldn't obtain paper id for {}", title);
        }
        else
            log.warn("Incomplete paper: {} : {}", title, authors);
    }
    
    private String getTextOnLine(int start, int end)
    {
        Area a = leaves.elementAt(start);
        StringBuilder sb = new StringBuilder(a.getText());
        int i = start + 1;
        while (i < end) //use all till end of line
        {
            Area next = leaves.elementAt(i);
            if (AreaUtils.isOnSameLineRoughly(a, next))
            {
                sb.append(next.getText());
                i++;
            }
            else
                break;
        }
        return sb.toString();
    }
    
    private String findPaperId(Area title)
    {
        Box box = title.getBoxes().firstElement();
        int top = box.getBounds().getY1();
        //try ID attribute
        String pid = null;
        Box cur = box;
        do
        {
            pid = cur.getAttribute("id");
            cur = cur.getParentBox();
        } while (pid == null && cur != null && cur.getBounds().getY1() - top < 20);
        if (pid != null)
        {
            //System.out.println("Found ID " + pid);
            return pid;
        }
        //try links
        String lnk = box.getAttribute("href");
        if (lnk != null)
        {
            lnk = lnk.trim();
            File file = new File("/", lnk);
            String name = file.getName();
            if (name.toLowerCase().endsWith(".pdf"))
                name = name.substring(0, name.length() - 4);
            else if (name.toLowerCase().endsWith(".ps"))
                name = name.substring(0, name.length() - 3);
            else if (name.toLowerCase().endsWith(".html"))
                name = name.substring(0, name.length() - 5);
            
            if (!name.isEmpty())
                return name;
            else
            {
                paperIdCnt++;
                return "paper" + paperIdCnt;
            }
        }
        else
            return null;
    }
    
}
