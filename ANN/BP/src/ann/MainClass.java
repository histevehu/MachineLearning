package ann;

import util.CLIProgressPrinter;
import util.DataUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Scanner;

public class MainClass
{
    public static void main(String[] args) throws Exception
    {
        DataUtil util = DataUtil.getInstance();
        Scanner scanner = new Scanner(System.in);
        System.out.print(">>>Training data file name(in the same directory as the program):\n  >");
        String trainfile = scanner.nextLine();
        System.out.print(">>>Training data attribute separator:\n  >");
        String separator = scanner.nextLine();
        List<DataNode> trainList = util.getDataList(trainfile, separator);
        System.out.print(">>>Training rate:\n  >");
        double eta = Double.valueOf(scanner.nextLine());
        System.out.print(">>>Training times:\n  >");
        int nIter = Integer.valueOf(scanner.nextLine());
        System.out.println("--------------------");
        System.out.print(">>>Test data file name(in the same directory as the program):\n  >");
        String testfile = scanner.nextLine();
        System.out.print(">>>Test data attribute separator:\n  >");
        separator = scanner.nextLine();
        List<DataNode> testList = util.getDataList(testfile, separator);
        System.out.print(">>>Test result output file name:\n  >");
        String outputfile = scanner.nextLine();
        BufferedWriter output = new BufferedWriter(new FileWriter(new File(
                outputfile)));
        //train bp ann
        int cateCount = util.getCateCount();
        CLIProgressPrinter cliProgressPrinter = new CLIProgressPrinter(nIter);
        BP bp = new BP(trainList.get(0).getAttrList().size(), cateCount);
        bp.setTrainNodes(trainList);
        bp.train(eta, nIter, cliProgressPrinter);
        //test bp ann
        int correctNum = 0;
        for (DataNode dataNode : testList)
        {
            int cateIndex = bp.test(dataNode);
            if (cateIndex == dataNode.getCategory())
            {
                correctNum++;
                output.write("√  ");
            } else
            {
                output.write("×  ");
            }
            output.flush();
            List<Double> attrs = dataNode.getAttrList();
            for (double attr : attrs)
            {
                output.write(attr + ",");
                output.flush();
            }
            output.write(util.getCateName(cateIndex) + (cateIndex == dataNode.getCategory() ? "" : " || " + util.getCateName(dataNode.getCategory())) + "\n");
            output.flush();
        }
        String outputContent = "===================="
                + "\nTraining file: " + trainfile
                + "\nTest file: " + testfile
                + "\nTest result output file: " + outputfile
                + "\n--------------------"
                + "\nTraining rate: " + eta
                + "\nTraining times: " + nIter
                + "\nCorrect rate: " + (double) correctNum / testList.size() + " [" + correctNum + "/" + testList.size() + "]";
        output.write(outputContent);
        output.flush();
        output.close();
        System.out.println(outputContent);
    }

}
