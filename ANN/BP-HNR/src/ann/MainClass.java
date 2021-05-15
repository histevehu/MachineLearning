package ann;

import util.CLIProgressPrinter;
import util.ImageUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class MainClass
{
    public static void main(String[] args) throws Exception
    {
        List<DataNode> trainList = new LinkedList<>();
        List<DataNode> testList = new LinkedList<>();
        ImageUtil imageUtil = ImageUtil.getInstance(28, 28, 28, 28);
        imageUtil.initial();
        System.out.print("</>Fetching train images info");
        imageUtil.fetchImgsInfo(ImageUtil.dataCate.TRAIN, imageUtil.getIm_proceed_train(), imageUtil.getDataNodes_train(), imageUtil.getW_uTrain(), imageUtil.getH_uTrain(), false);
        System.out.print("</>Fetching test images info");
        imageUtil.fetchImgsInfo(ImageUtil.dataCate.TEST, imageUtil.getIm_proceed_test(), imageUtil.getDataNodes_test(), imageUtil.getW_uTest(), imageUtil.getH_uTest(), false);
        for (int i = 0; i <= 9; i++)
        {
            for (DataNode dataNode : imageUtil.getDataNodes_train().get(i))
                trainList.add(dataNode);
        }
        for (int i = 0; i <= 9; i++)
        {
            for (DataNode dataNode : imageUtil.getDataNodes_test().get(i))
                testList.add(dataNode);
        }
        //set attributes of BP training
        Scanner scanner = new Scanner(System.in);
        System.out.print(">>>Training rate:\n  >");
        double eta = Double.valueOf(scanner.nextLine());
        System.out.print(">>>Training times:\n  >");
        int nIter = Integer.valueOf(scanner.nextLine());
        System.out.print(">>>Test result output file name:\n  >");
        String outputfile = scanner.nextLine();
        BufferedWriter output = new BufferedWriter(new FileWriter(new File(
                outputfile)));
        //train bp ann
        int cateCount = 10;
        CLIProgressPrinter cliProgressPrinter = new CLIProgressPrinter(nIter);
        System.out.println("</>Training BP");
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
            /*List<Double> attrs = dataNode.getAttrList();
            for (double attr : attrs)
            {
                output.write(attr + ",");
                output.flush();
            }*/
            output.write(cateIndex + "  ||  " + dataNode.getCategory() + "\n");
            output.flush();
        }
        String outputContent = "===================="
                + "\nTest result output file: " + outputfile
                + "\n--------------------"
                + "\nInput layer nodes: " + bp.getmInputCount()
                + "\nHidden layer nodes: " + bp.getmHiddenCount()
                + "\nOutput layer nodes: " + bp.getmOutputCount()
                + "\nTraining rate: " + eta
                + "\nTraining times: " + nIter
                + "\nCorrect rate: " + (double) correctNum / testList.size() + " [" + correctNum + "/" + testList.size() + "]";
        output.write(outputContent);
        output.flush();
        output.close();
        System.out.println(outputContent);
    }

}
