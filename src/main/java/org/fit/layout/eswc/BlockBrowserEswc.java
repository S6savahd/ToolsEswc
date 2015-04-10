/**
 * BlockBrowserTest.java
 *
 * Created on 24. 2. 2015, 9:21:16 by burgetr
 */

package org.fit.layout.eswc;

import java.awt.EventQueue;
import java.net.URL;

import javax.swing.JFrame;

import org.fit.layout.api.AreaTreeOperator;
import org.fit.layout.classify.op.TagEntitiesOperator;
import org.fit.layout.classify.op.VisualClassificationOperator;
import org.fit.layout.eswc.classify.ProgrammesFeatureExtractor;
import org.fit.layout.tools.BlockBrowser;

/**
 * 
 * @author burgetr
 */
public class BlockBrowserEswc extends BlockBrowser
{

    public BlockBrowserEswc()
    {
        super();
        
        AreaTreeOperator vcls = getProcessor().getOperators().get("FitLayout.Tag.Visual");
        if (vcls != null && vcls instanceof VisualClassificationOperator)
        {
            ((VisualClassificationOperator) vcls).setFeatures(new ProgrammesFeatureExtractor());
        }
        else
            System.err.println("Couldn't configure FitLayout.Tag.Visual!");
        
        AreaTreeOperator tcls = getProcessor().getOperators().get("FitLayout.Tag.Entities");
        if (tcls != null && tcls instanceof TagEntitiesOperator)
        {
            ((TagEntitiesOperator) tcls).addTagger(new CountriesTagger());
            ((TagEntitiesOperator) tcls).addTagger(new ShortNameTagger());
        }
        else
            System.err.println("Couldn't configure FitLayout.Tag.Entities!");
    }

    @Override
    public void initPlugins()
    {
        super.initPlugins();
        getSegmAutorunCheckbox().setSelected(true);
        getLogicalAutorunCheckbox().setSelected(true);
    }    
    
    public static void main(String[] args)
    {
        EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
                try
                {
                    browser = new BlockBrowserEswc();
                    browser.setLoadImages(false);
                    JFrame main = browser.getMainWindow();
                    
                    //main.setSize(1000,600);
                    //main.setMinimumSize(new Dimension(1200, 600));
                    //main.setSize(1500,600);
                    main.setSize(1600,1000);
                    browser.initPlugins();
                    main.setVisible(true);
                    
                    //String localpath = "file://" + System.getProperty("user.home");
                    //localpath += "/git/TestingLayout";
            
                    
                    /* CEUR */
                    //URL url = new URL(localpath + "/test/ceur/volumes/Vol-1317.html");
                    //URL url = new URL("http://ceur-ws.org/Vol-1317/");
                    //URL url = new URL("http://ceur-ws.org/Vol-585/");
                    //URL url = new URL("http://ceur-ws.org/Vol-540/");
                    //URL url = new URL("http://ceur-ws.org/Vol-250/");
                    //URL url = new URL("http://ceur-ws.org/Vol-164/"); //(tocna)
                    //URL url = new URL("http://ceur-ws.org/Vol-104/");
                    URL url = new URL("http://ceur-ws.org/Vol-100/"); //TODO editori
                    //URL url = new URL("http://ceur-ws.org/Vol-15/"); //TODO total chaos
                    //URL url = new URL("http://ceur-ws.org/Vol-5/");
                    //URL url = new URL("http://ceur-ws.org/Vol-1/");
                    //URL url = new URL("");
                    //URL url = new URL("");
                    //URL url = new URL("");
                        
                    browser.setLocation(url.toString());
                        
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

}
