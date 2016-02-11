package org.getalp.lexsema.wsd.method;

import java.util.HashMap;

import org.getalp.lexsema.io.annotresult.SemevalWriter;
import org.getalp.lexsema.similarity.Document;
import org.getalp.lexsema.wsd.configuration.Configuration;
import org.getalp.lexsema.wsd.configuration.ContinuousConfiguration;

public class VoteDisambiguator implements Disambiguator
{
    private Disambiguator disambiguator;
    
    private int n;
    
    private String ansDirectory;
    
    public VoteDisambiguator(Disambiguator disambiguator, int n, String ansDirectory)
    {
        this.disambiguator = disambiguator;
        this.n = n;
        this.ansDirectory = ansDirectory;
    }

    public Configuration disambiguate(Document document)
    {
        int nbWords = document.size();
        
        Configuration[] configurations = new Configuration[n];
        for (int i = 0 ; i < n ; i++)
        {
            System.out.println("" + i + "/" + n + "...");
            configurations[i] = disambiguator.disambiguate(document);
            if (ansDirectory != null)
            {
                SemevalWriter sw = new SemevalWriter(ansDirectory + "/" + document.getId() + "_" + i + ".ans");
                sw.write(document, configurations[i].getAssignments());
            }
        }

        int[] finalSenses = new int[nbWords];
        for (int i = 0 ; i < nbWords ; i++)
        {
            HashMap<Integer, Integer> candidates = new HashMap<>();
            for (int j = 0 ; j < n ; j++)
            {
                int assignment = configurations[j].getAssignment(i);
                if (candidates.containsKey(assignment))
                {
                    int oldValue = candidates.get(assignment);
                    candidates.put(assignment, oldValue + 1);
                }
                else
                {
                    candidates.put(assignment, 0);
                }
            }
            int maxKey = -1;
            int maxValue = -1;
            for (Integer j : candidates.keySet())
            {
                if (candidates.get(j) > maxValue)
                {
                    maxKey = j;
                    maxValue = candidates.get(j);
                }
            }
            finalSenses[i] = maxKey;
        }
        
        Configuration finalConfiguration = new ContinuousConfiguration(document, finalSenses);

        if (ansDirectory != null)
        {
            SemevalWriter sw = new SemevalWriter(ansDirectory + "/" + document.getId() + "_fusion.ans");
            sw.write(document, finalConfiguration.getAssignments());
        }
        
        return finalConfiguration;
    }
    
    public Configuration disambiguate(Document document, Configuration c)
    {
        return disambiguate(document);
    }
    
    public void release()
    {
        disambiguator.release();
    }
}
