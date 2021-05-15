package util;

import ann.DataNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class DataUtil
{
    private static DataUtil instance = null;
    //storage categories of data <category name, category index>
    private Map<String, Integer> mCates;
    //number of categories
    private int cateCount;

    private DataUtil()
    {
        mCates = new HashMap<>();
        cateCount = 0;
    }

    public static synchronized DataUtil getInstance()
    {
        if (instance == null)
            instance = new DataUtil();
        return instance;
    }

    public Map<String, Integer> getmCates()
    {
        return mCates;
    }

    public int getCateCount()
    {
        return cateCount;
    }

    public String getCateName(int cateIndex)
    {
        if (cateIndex == -1)
            return "<!>Illegal category index: " + cateIndex;
        Iterator<String> keys = mCates.keySet().iterator();
        while (keys.hasNext())
        {
            String key = keys.next();
            if (mCates.get(key) == cateIndex)
                return key;
        }
        return null;
    }

    /**
     * Generate the training list from the file.
     * CAUTION: The program will use the first non-numeric attributes as the categories name
     *
     * @param fileName data file name
     * @param sep      the separator between each attribute of the data
     * @return data node list of data
     */

    public List<DataNode> getDataList(String fileName, String sep)
            throws Exception
    {
        List<DataNode> list = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(new File("./data/" + fileName)));
        String line;
        while ((line = br.readLine()) != null)
        {
            String splits[] = line.split(sep);
            DataNode node = new DataNode();
            for (int i = 0; i < splits.length; i++)
            {
                try
                {
                    node.addAttr(Double.valueOf(splits[i]));
                }
                // If it is not a number, it is the category name, put it and its index to mTypes
                catch (NumberFormatException e)
                {
                    //If the category of data is a new category, add the category to map mCates
                    if (!mCates.containsKey(splits[i]))
                    {
                        mCates.put(splits[i], cateCount++);
                    }
                    //set the node category of the data
                    node.setCategory(mCates.get(splits[i]));
                    list.add(node);
                }
            }
        }
        return list;
    }
}
