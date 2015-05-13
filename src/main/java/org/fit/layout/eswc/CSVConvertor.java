/**
 * CSVConvertor.java
 *
 * Created on 7. 5. 2015, 13:42:48 by burgetr
 */
package org.fit.layout.eswc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import org.fit.layout.classify.taggers.DateTagger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author burgetr
 */
public class CSVConvertor
{
    private static Logger log = LoggerFactory.getLogger(CSVConvertor.class);

    //stored triples: volume URL -> predicate -> list of strings
    private Map<String, Map<String, List<String>>> idata;
    
    public CSVConvertor() 
    {
        idata = new HashMap<String, Map<String, List<String>>>();
    }
    
    private void store(String s, String p, String o)
    {
        Map<String, List<String>> voldata = idata.get(s);
        if (voldata == null)
        {
            voldata = new HashMap<String, List<String>>();
            idata.put(s, voldata);
        }
        List<String> vals = voldata.get(p);
        if (vals == null)
        {
            vals = new Vector<String>();
            voldata.put(p, vals);
        }
        vals.add(o);
    }
    
    private void out(String s, String p, String o)
    {
        //System.out.println(s + " " + p + " \"" + o + "\" .");
        o = o.replaceAll("\"", "\\\\\"");
        store(s, p, "\"" + o + "\"");
    }
    
    private void outurl(String s, String p, String o)
    {
        //System.out.println(s + " " + p + " <" + o + "> .");
        store(s, p, "<" + o + ">");
    }
    
    public List<String> getData(int vol, String pred)
    {
        String volstr = "<http://ceur-ws.org/Vol-" + vol + "/>";
        Map<String, List<String>> vdata = idata.get(volstr);
        if (vdata != null)
        {
            List<String> pdata = vdata.get(pred);
            if (pdata != null)
                return pdata;
        }
        return Collections.emptyList();
    }
    
    public void dump(String destfile)
    {
        try
        {
            PrintWriter w = new PrintWriter(destfile);
            for (Map.Entry<String, Map<String, List<String>>> ventry : idata.entrySet())
            {
                for (Map.Entry<String, List<String>> dentry : ventry.getValue().entrySet())
                {
                    for (String val : dentry.getValue())
                        w.println(ventry.getKey() + " " + dentry.getKey() + " " + val + " .");
                }
            }
            w.close();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }
    
    public void parseIndex(BufferedReader in) throws IOException
    {
        String line;
        while ((line = in.readLine()) != null)
        {
            //volume title
            String[] f = line.split(";;");
            out(f[0], "segm:ititle", f[1]);
            //System.err.println(f[0]);
            
            //date of publication
            DateFormat srcf = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
            DateFormat dfmt = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            try
            {
                Date pubdate = srcf.parse(f[2]);
                out(f[0], "segm:isubmitted", dfmt.format(pubdate));
            } catch (ParseException e)
            {
                log.error("Couldn't decode " + f[2] + ": " + e.getMessage());
            }

            //proceedings text 
            out(f[0], "segm:iproceedings", f[3]);
            //date and place text
            out(f[0], "segm:idateplace", f[4]);

            //start-end date
            DateTagger dt = new DateTagger();
            List<Date> dates = dt.extractDates(f[4]);
            if (dates.size() == 1)
            {
                out(f[0], "segm:istartdate", dfmt.format(dates.get(0)));
                out(f[0], "segm:ienddate", dfmt.format(dates.get(0)));
            }
            else if (dates.size() == 2)
            {
                out(f[0], "segm:istartdate", dfmt.format(dates.get(0)));
                out(f[0], "segm:ienddate", dfmt.format(dates.get(1)));
            }
            else
                log.warn("Strange number of date values: {}", f[4]);
            
            //country
            CountriesTagger ct = new CountriesTagger();
            List<String> countries = ct.extract(f[4]);
            if (countries.size() >= 1)
            {
                String uri = Countries.getCountryUri(countries.get(countries.size() - 1)); //use the last country
                outurl(f[0], "segm:country", uri);
            }

            //short titles
            SubtitleParser sp = new SubtitleParser(f[3], new Vector<String>());
            for (Event ev : sp.getWorkshops())
            {
                int o = (ev.order > 0) ? ev.order : 1;
                out(f[0], "segm:ishort", ev.sname + ":" + o);
            }
            Event coloc = sp.getColocEvent();
            if (coloc != null)
                out(f[0], "segm:icoloc", coloc.sname);
            
        }
    }
    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        if (args.length != 2)
        {
            System.err.println("Usage: CSVConvertor <input_file.csv> <outfile.n3>");
            System.exit(1);
        }
        
        try
        {
            CSVConvertor csv = new CSVConvertor();
            BufferedReader in = new BufferedReader(new FileReader(args[0]));
            csv.parseIndex(in);
            in.close();
            csv.dump(args[1]);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}