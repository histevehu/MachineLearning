import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NBC
{
    //attribute name
    private ArrayList<String> attribute = new ArrayList<>();
    //the values of each attribute
    private ArrayList<ArrayList<String>> attributeValue = new ArrayList<>();
    //original data
    private ArrayList<String[]> data = new ArrayList<>();
    //index of the target attribute, which is the attribute of the final decision
    int tgtAttribute;
    //The number of data corresponding to each target attribute value
    private int[] PPN;
    //Conditional probability CP[attribute index][attribute value index][target attribute index of attribute value]
    private double[][][] CP;
    //map<[attribute index,target attribute index],ave or deviation>
    //CAUTION:if the type of key replaced with array, the get(array) method will return null and the data cannot be obtained
    Map<List<Integer>, Double> ave, dev;
    //regular expression used to match the header "@attribute" part of the data file
    public static final String patternString_discrete = "@attribute(.*)[{](.*?)[}]";
    public static final String patternString_continuous = "@attribute(.*)[ ](.*)";
    //indicate if data includes continuous data
    boolean hasConData = false;

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
                        String[] row = line.split(",");
                        data.add(row);
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
            System.err.println("<!>Data file read failed.");
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

    public double calcAve(double[] arr)
    {
        double sum = 0;
        for (double a : arr)
            sum += a;
        return sum / arr.length;
    }

    public double calcDev(double[] arr)
    {
        double ave = calcAve(arr);
        double sum = 0;
        for (double a : arr)
            sum += Math.pow((a - ave), 2);
        return sum / (arr.length - 1);
    }

    public double calcStdev(double dev)
    {
        return Math.sqrt(dev);
    }

    /**
     * calculate gauss probability
     *
     * @param attrIndex         the attribute class index of the value
     * @param tgtAttrValueIndex the index of the target attribute class
     * @param x                 value
     * @return
     */
    public double calcGaussProb(int attrIndex, int tgtAttrValueIndex, double x)
    {
        double exponent = Math.exp(-(Math.pow(x - ave.get(Arrays.asList(attrIndex, tgtAttrValueIndex)), 2)) / (2 * dev.get(Arrays.asList(attrIndex, tgtAttrValueIndex))));
        double gaussProb = (1 / (Math.sqrt(2 * Math.PI) * calcStdev(dev.get(Arrays.asList(attrIndex, tgtAttrValueIndex))))) * exponent;
        return gaussProb;
    }

    /**
     * calculate class prior probability & conditional probability (just discrete type attributes) & averages and deviations of continuous attributes
     * the conditional probability of continuous type attributes need to be calculated according to the input test value during the test (test())
     */
    public void calcProbs()
    {
        //initialize PP and CP array
        PPN = new int[attributeValue.get(tgtAttribute).size()];
        CP = new double[attribute.size()][][];
        for (int i = 0; i < attribute.size(); i++)
            if (attributeValue.get(i) != null)
                CP[i] = new double[attributeValue.get(i).size()][attributeValue.get(tgtAttribute).size()];
        //calc PP
        for (String[] d : data)
            PPN[attributeValue.get(tgtAttribute).indexOf(d[tgtAttribute])]++;

        //calc CP
        //EACH TARGET ATTRIBUTE OF ATTRIBUTE VALUE
        for (int i = 0; i < attributeValue.get(tgtAttribute).size(); i++)
        {
            double tgtNum = PPN[i];
            //EACH ATTRIBUTE
            for (int j = 0; j < attribute.size(); j++)
            {
                //if the attribute is target attribute or the the type of the attribute is continuous,skip this loop,turn to next attribute
                if (j == tgtAttribute || attributeValue.get(j) == null)
                    continue;
                //EACH ATTRIBUTE VALUE
                for (int k = 0; k < attributeValue.get(j).size(); k++)
                {
                    int n = 0;
                    for (String[] d : data)
                    {
                        if (d[j].equals(attributeValue.get(j).get(k)) && d[tgtAttribute].equals(attributeValue.get(tgtAttribute).get(i)))
                            n++;
                    }
                    CP[j][k][i] = (double) n / tgtNum;
                }
            }
        }
        //if has continuous type attribute in data,initialize average and deviation var maps and calc them for GaussProb calculation in test()
        if (hasConData)
        {
            ave = new HashMap<>();
            dev = new HashMap<>();
            for (int i = 0; i < attribute.size(); i++)
            {
                //this attribute type is continuous
                if (attributeValue.get(i) == null)
                {
                    //add the sub data of the attribute in the data to an array according to the target attribute values category
                    for (int j = 0; j < attributeValue.get(tgtAttribute).size(); j++)
                    {

                        double[] arr = new double[PPN[j]];
                        int n = 0;
                        for (int k = 0; k < data.size(); k++)
                        {
                            if (n == arr.length)
                                break;
                            if (data.get(k)[tgtAttribute].equals(attributeValue.get(tgtAttribute).get(j)))
                                arr[n++] = Double.parseDouble(data.get(k)[i]);
                        }
                        //calculate and store the average and deviation of the continuous type attribute
                        ave.put(Arrays.asList(i, j), calcAve(arr));
                        dev.put(Arrays.asList(i, j), calcDev(arr));
                    }

                }
            }
        }
        return;
    }

    /**
     * @param testData the length=attribute.size(), the target attribute value is null
     * @return
     */
    public int test(String[] testData)
    {
        int rs = -1;
        double max = Double.MIN_VALUE;
        for (int i = 0; i < attributeValue.get(tgtAttribute).size(); i++)
        {
            //tmp:prior probability of category (PPN/number of total data)
            double tmp = (double) PPN[i] / data.size();
            for (int j = 0; j < attribute.size(); j++)
            {
                if (j == tgtAttribute)
                    continue;
                tmp *= (attributeValue.get(j) == null ? calcGaussProb(j, i, Double.parseDouble(testData[j])) : CP[j][attributeValue.get(j).indexOf(testData[j])][i]);
            }
            if (tmp > max)
            {
                rs = i;
                max = tmp;
            }
        }
        return rs;
    }

    public static void main(String[] args)

    {
        NBC nbc = new NBC();
        Scanner scanner = new Scanner(System.in);
        //read .arff data file
        System.out.print(">>>Data file name(ONLY *.arff format, ONLY search in the same directory as the program):\n" +
                ">>>(E.g:file \"test.arff\" just need to input \"test\")\n" +
                "  >");
        String fileName = scanner.nextLine();
        File file = new File("./data/" + fileName + ".arff");
        nbc.readARFF(file);
        //input target attribute
        System.out.print(">>>Target attribute:\n  >");
        //check and set the index of target attribute to tgtAttribute
        nbc.setTgtAttribute(scanner.nextLine());
        nbc.calcProbs();
        //test
        System.out.println(">>>Test example input:");
        String[] t = new String[nbc.attribute.size()];
        /*quick test data(watermelon.txt):
        {"青绿", "蜷缩", "浊响", "清晰", "凹陷", "硬滑", "0.697", "0.460", null};*/
        for (int i = 0; i < t.length; i++)
        {
            if (i == nbc.tgtAttribute)
                continue;
            System.out.print("  >Attribute: " + nbc.attribute.get(i) + "\n  >");
            t[i] = scanner.nextLine();
            if (nbc.attributeValue.get(i) != null && nbc.attributeValue.get(i).indexOf(t[i]) == -1)
            {
                System.out.println("<!>Input test value of attribute(" + nbc.attribute.get(i) + ") doesn't exist.");
                System.exit(-1);
            }
        }
        System.out.println(">>>Test example target attribute value("
                + nbc.attribute.get(nbc.tgtAttribute) + "): "
                + nbc.attributeValue.get(nbc.tgtAttribute).get(nbc.test(t)));
        System.out.println();
    }

}
