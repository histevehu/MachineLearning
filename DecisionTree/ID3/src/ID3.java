import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ID3
{
    //attribute name
    private ArrayList<String> attribute = new ArrayList<String>();
    //the values of each attribute
    private ArrayList<ArrayList<String>> attributeValue = new ArrayList<ArrayList<String>>();
    //original data
    private ArrayList<Object[]> data = new ArrayList<>();
    //index of the target attribute, which is the attribute of the final decision
    int tgtAttribute;
    //regular expression used to match the header "@attribute" part of the data file
    public static final String patternString_discrete = "@attribute(.*)[{](.*?)[}]";
    public static final String patternString_continuous = "@attribute(.*)[ ](.*)";
    //indicate if data includes continuous data
    boolean hasConData = false;
    //thresholds of continuous data map<index of attribute,threshold>
    Map<Integer, Double> thresholdMap = new HashMap<>();
    //output xml file
    Document xmldoc;
    //root node of the output xml file, which is also the node of the decision tree
    Element root;

    //initialize decision tree
    public ID3()
    {
        xmldoc = DocumentHelper.createDocument();
        root = xmldoc.addElement("root");
        root.addElement("DecisionTree").addAttribute("value", "null");
    }

    public static void main(String[] args)
    {
        ID3 id3 = new ID3();
        Scanner scanner = new Scanner(System.in);
        //read .arff data file
        System.out.print(">>>Data file name(ONLY *.arff format, ONLY search in the same directory as the program):\n  >");
        String fileName = scanner.nextLine();
        File file = new File("./data/" + fileName + ".arff");
        id3.readARFF(file);
        //input target attribute
        System.out.print(">>>Target attribute:\n  >");
        //check and set the index of target attribute to tgtAttribute
        id3.setTgtAttribute(scanner.nextLine());
        //store indexes of attributes(except target attribute)
        LinkedList<Integer> attList = new LinkedList<Integer>();
        for (int i = 0; i < id3.attribute.size(); i++)
        {
            if (i != id3.tgtAttribute)
                attList.add(i);
        }
        //store indexes of original data
        ArrayList<Integer> dataNumList = new ArrayList<Integer>();
        for (int i = 0; i < id3.data.size(); i++)
        {
            dataNumList.add(i);
        }
        //build decision tree
        id3.buildDT("DecisionTree", "null", dataNumList, attList);
        //output decision tree to xml file
        id3.writeXML(fileName + "-dt.xml");
        return;
    }

    //read .arff file and initialize attribute,attributeValue,data
    public void readARFF(File file)
    {
        try
        {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;
            Scanner scanner = new Scanner(System.in);
            //initialize two patterns for discrete and continuous data
            Pattern pattern_dis = Pattern.compile(patternString_discrete);
            Pattern pattern_con = Pattern.compile(patternString_continuous);
            while ((line = br.readLine()) != null)
            {
                //initialize two matchers for discrete and continuous data
                Matcher matcher_dis = pattern_dis.matcher(line);
                Matcher matcher_con = pattern_con.matcher(line);
                //if matched discrete attribute head, group(1): attribute name, group(2): attribute values
                if (matcher_dis.find())
                {
                    attribute.add(matcher_dis.group(1).trim());
                    String[] values = matcher_dis.group(2).split(",");
                    ArrayList<String> al = new ArrayList<>(values.length);
                    for (String value : values)
                        al.add(value.trim());
                    //attributevalue element type:string[]
                    attributeValue.add(al);
                } else if (matcher_con.find())
                {
                    if (matcher_con.group(2).equals("numeric"))
                    {
                        attribute.add(matcher_con.group(1).trim());
                        //attributevalue element type:null
                        //value null will be used later to distinguish whether the attribute corresponding to the attribute value is continuous(null) or discrete(string[]).
                        attributeValue.add(null);
                        hasConData = true;
                        //input threshold of current continuous attribute
                        //Only supports one threshold temporarily, that is, two categories
                        System.out.print(">>>Input threshold of data \"" + matcher_con.group(1).trim() + "\"\n  >");
                        thresholdMap.put((attribute.size() - 1), Double.valueOf(scanner.next()));
                    } else
                    {
                        System.err.println("<!>Data file includes unsupported type of data");
                        System.exit(-1);
                    }
                }
                //else check if in @data part of file
                else if (line.startsWith("@data"))
                {
                    while ((line = br.readLine()) != null)
                    {
                        if (line == "") //skip blank lines
                            continue;
                        //add data
                        if (hasConData)
                        {
                            String[] row = line.split(",");
                            Object[] t_data = new Object[attributeValue.size()];
                            for (int i = 0; i < attributeValue.size(); i++)
                            {
                                t_data[i] = (attributeValue.get(i) == null) ? Double.valueOf(row[i]) : row[i];
                            }
                            data.add(t_data);
                        } else
                        {
                            String[] row = line.split(",");
                            data.add(row);
                        }
                    }
                } else //skip blank lines
                {
                    continue;
                }
            }
            br.close();
        } catch (FileNotFoundException fnfe)
        {
            System.err.println("<!>Data file not found in current directory(not include sub directories)");
            System.exit(-1);
        } catch (Exception e)
        {
            //System.err.println("<!>Data file (" + fileName + ".arff" + ") read failed.");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    //check and set the index of target attribute to tgtAttribute
    public void setTgtAttribute(String name)
    {
        int n = attribute.indexOf(name);
        if (n < 0 || n >= attribute.size())
        {
            System.err.println("<!>Target attribute doesn't exist.");
            System.exit(-1);
        }
        tgtAttribute = n;
    }

    //Calculate the information entropy of a single attribute value branch

    /**
     * @param info statistical records of the attribute branch needed to calc entropy
     * @return
     */
    public double getEntropy(int[] info)
    {
        double entropy = 0.0;
        int sum = 0;
        for (int i = 0; i < info.length; i++)
        {
            entropy -= info[i] * Math.log(info[i] + Double.MIN_VALUE) / Math.log(2);
            sum += info[i];
        }
        entropy += sum * Math.log(sum + Double.MIN_VALUE) / Math.log(2);
        entropy /= sum;
        return entropy;
    }

    //check whether the data is pure
    //if all target attributes in the data set are the same, it means that data all belong to the same category, then the data set is pure
    public boolean infoPure(ArrayList<Integer> subDataIndex)
    {
        //Regardless of whether the category attribute value type of the current data set is string or number, convert it to a string for comparison
        String value = (String) data.get((subDataIndex.get(0)))[tgtAttribute];
        for (int i = 1; i < subDataIndex.size(); i++)
        {
            String next = (String) data.get(subDataIndex.get(i))[tgtAttribute];
            if (!value.equals(next))
                return false;
        }
        return true;
    }

    /**
     * @param subDataIndex indexes of sub data
     * @param index        index of attribute to calculate information entropy
     * @return information entropy of the attribute = sum of (the information entropy of each branch of the attribute * the proportion of the number of branch of the attribute to the total number of data of the attribute)
     */
    public double calcAttributeEntropy(ArrayList<Integer> subDataIndex, int index)
    {
        //total number of data of the attribute
        int sum = subDataIndex.size();
        double entropy = 0.0;
        // info[][] for statistical records when traversing data later
        // If the attribute corresponding to index is a discrete type: info[number of attribute value branches][target attribute number of each attribute value branch]
        // If the attribute corresponding to index is a continuous type: info[2(only supports one threshold temporarily)][target attribute number of each attribute value branch]
        // If attributeValue.get(index) != null then type of attribute(index) is discrete, or it's continuous
        int[][] info = new int[(attributeValue.get(index) != null) ? attributeValue.get(index).size() : 2][];
        for (int i = 0; i < info.length; i++)
            info[i] = new int[attributeValue.get(tgtAttribute).size()];
        //Count[] store the number of data corresponding to each attribute value branch
        int[] count = new int[(attributeValue.get(index) != null) ? attributeValue.get(index).size() : 2];

        //start to traverse data
        //discrete data
        if (attributeValue.get(index) != null)
        {
            for (int i = 0; i < sum; i++)
            {
                //n is the index of the current data
                int n = subDataIndex.get(i);
                //Get the index of the attribute needed to calc entropy of current data
                int t_data_att_index = attributeValue.get(index).indexOf(data.get(n)[index]);
                //Get the index of the target attribute of the current data
                int t_data_tgtAtt_index = attributeValue.get(tgtAttribute).indexOf(data.get(n)[tgtAttribute]);
                count[t_data_att_index]++;
                info[t_data_att_index][t_data_tgtAtt_index]++;
            }
        }
        //continuous data
        else
        {
            for (int i = 0; i < sum; i++)
            {
                //n is the index of the current data
                int n = subDataIndex.get(i);
                //According to whether the value > threshold value to classify, value > threshold: category 1; value <= threshold: category 0
                int t_data_att_index = Double.valueOf(data.get(n)[index].toString()) <= thresholdMap.get(index) ? 0 : 1;
                //Get the index of the target attribute of the current data
                int t_data_tgtAtt_index = attributeValue.get(tgtAttribute).indexOf(data.get(n)[tgtAttribute]);
                count[t_data_att_index]++;
                info[t_data_att_index][t_data_tgtAtt_index]++;
            }
        }

        //calc the sum of (the information entropy of each branch of the attribute * the proportion of the number of branch of the attribute to the total number of data of the attribute)
        for (int i = 0; i < info.length; i++)
        {
            entropy += getEntropy(info[i]) * count[i] / sum;
        }
        return entropy;
    }


    //Take nodename as the parent node, construct the sub node whose attribute is value (that is, the current attribute)

    /**
     * @param nodeName     parent attribute node value
     * @param value        current attribute node value
     * @param subDataIndex indexes of sub data
     * @param attList      the remaining attributes not included in the decision tree
     */
    public void buildDT(String nodeName, String value, ArrayList<Integer> subDataIndex,
                        LinkedList<Integer> attList)
    {
        Element ele = null;
        @SuppressWarnings("unchecked")
        //locate the parent node based on the root node
        //and store all existing sub nodes under the parent node to a list
        List list = root.selectNodes("//" + nodeName);
        Iterator<Element> iter = list.iterator();
        //traverse the list, locate ele to the node that needs to be operated this time, that is, the node of current attribute)
        //subsequent operations will be based on this node
        while (iter.hasNext())
        {
            ele = iter.next();
            if (ele.attributeValue("value").equals(value))
                break;
        }
        //check whether the sub data is pure
        //if it is, it means that the current node pointed to by ele is a leaf node, skip the subsequent calc and return directly
        if (infoPure(subDataIndex))
        {
            ele.setText((String) data.get(subDataIndex.get(0))[tgtAttribute]);
            return;
        }
        //data isn't pure
        //calc the information gain of each remaining attributes, the attribute with the largest information gain will be selected as the current node value pointed to by ele
        //and because attribute gain=parent node entropy-attribute entropy
        //to obtain the maximum attribute gain, just need to get the minimum attribute entropy.

        //minIndex records the index of attribute with smallest entropy
        int minIndex = -1;
        double minEntropy = Double.MAX_VALUE;
        //traverse the list of remaining attributes and calc information entropy of each attribute
        for (int i = 0; i < attList.size(); i++)
        {
            if (i == tgtAttribute)//if it is the target attribute then skip
                continue;
            double entropy = calcAttributeEntropy(subDataIndex, attList.get(i));
            //If present entropy is the smallest, record the current attribute
            if (entropy < minEntropy)
            {
                minIndex = attList.get(i);
                minEntropy = entropy;
            }
        }
        //The sub pointed to by the parent node(ele), its value will be the attribute name with the smallest information entropy
        String subNodeName = attribute.get(minIndex);
        //Remove the attribute which was included in the decision tree this time from the list of remaining attributes
        attList.remove(Integer.valueOf(minIndex));
        //Add all attribute values of the current attribute to the ele node
        //type of attribute with smallest entropy is continuous
        if (attributeValue.get(minIndex) == null)
        {
            for (int i = 0; i < 2; i++)
            {
                //Find the data which its attribute value <= or > threshold separately and add them to al
                //as a sub data set for recursively constructing decision branch of each attribute value branch
                ArrayList<Integer> al = new ArrayList<Integer>();
                double val = thresholdMap.get(minIndex);
                //first loop：find the data which its attribute value <= threshold
                //second loop：find the data which its attribute value > threshold
                String subNodeAtt = ((i == 0) ? "LtOrEt:" : "Gt:") + String.valueOf(val);
                ele.addElement(subNodeName).addAttribute("value", subNodeAtt);
                for (int j = 0; j < subDataIndex.size(); j++)
                {
                    switch (i)
                    {
                        case 0:
                            if ((double) data.get(subDataIndex.get(j))[minIndex] <= val)
                                al.add(subDataIndex.get(j));
                            break;
                        case 1:
                            if ((double) data.get(subDataIndex.get(j))[minIndex] > val)
                                al.add(subDataIndex.get(j));
                            break;
                    }
                }
                //recursively constructing decision branches
                buildDT(subNodeName, subNodeAtt, al, attList);
            }

        }
        //type of attribute with smallest entropy is discrete
        else
        {
            ArrayList<String> attvalues = attributeValue.get(minIndex);
            for (String val : attvalues)
            {
                //Find the data of attribute with the smallest information entropy according to the type and add it to al
                //as a sub data set for recursively constructing decision branch of each attribute value branch
                ArrayList<Integer> al = new ArrayList<Integer>();
                ele.addElement(subNodeName).addAttribute("value", val);
                for (int i = 0; i < subDataIndex.size(); i++)
                {
                    if (data.get(subDataIndex.get(i))[minIndex].equals(val))
                    {
                        al.add(subDataIndex.get(i));
                    }
                }
                //recursively constructing decision branches
                buildDT(subNodeName, val, al, attList);
            }
        }

    }

    //output decision tree to xml file
    public void writeXML(String filename)
    {
        try
        {
            File file = new File(("./data/" + filename));
            if (!file.exists())
                file.createNewFile();
            FileWriter fw = new FileWriter(file);
            OutputFormat format = OutputFormat.createPrettyPrint(); // 美化格式
            XMLWriter output = new XMLWriter(fw, format);
            output.setEscapeText(false);
            output.write(xmldoc);
            output.close();
            System.out.println(">>>The decision tree has been saved in \"" + filename + "\"");
        } catch (IOException e)
        {
            System.out.println(e.getMessage());
        }
    }
}
