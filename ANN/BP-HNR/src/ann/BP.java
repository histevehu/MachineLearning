package ann;

import util.CLIProgressPrinter;

import java.util.ArrayList;
import java.util.List;

public class BP
{
    private int mInputCount;
    private int mHiddenCount;
    private int mOutputCount;

    private List<NetworkNode> mInputNodes;
    private List<NetworkNode> mHiddenNodes;
    private List<NetworkNode> mOutputNodes;

    private double[][] mInputHiddenWeight;
    private double[][] mHiddenOutputWeight;

    private List<DataNode> trainNodes;

    public int getmInputCount()
    {
        return mInputCount;
    }

    public int getmHiddenCount()
    {
        return mHiddenCount;
    }

    public int getmOutputCount()
    {
        return mOutputCount;
    }

    public void setTrainNodes(List<DataNode> trainNodes)
    {
        this.trainNodes = trainNodes;
    }

    /**
     * @param inputCount  number of nodes in input layer
     * @param outputCount number of nodes in output layer
     * @param hiddenCount number of nodes in hidden layer
     */
    public BP(int inputCount, int outputCount, int hiddenCount)
    {
        trainNodes = new ArrayList<>();
        mInputCount = inputCount;
        mHiddenCount = hiddenCount;
        mOutputCount = outputCount;
        mInputNodes = new ArrayList<>();
        mHiddenNodes = new ArrayList<>();
        mOutputNodes = new ArrayList<>();
        mInputHiddenWeight = new double[inputCount][hiddenCount];
        mHiddenOutputWeight = new double[mHiddenCount][mOutputCount];
    }

    /**
     * @param inputCount  number of nodes in input layer
     * @param outputCount number of nodes in output layer
     *                    The number of hidden layer nodes is (sqrt(inputCount+outputCount))+a,a range [1,10], the default is 10
     */
    public BP(int inputCount, int outputCount)
    {
        int hiddenCount = (int) Math.floor((Math.sqrt(inputCount + outputCount))) + 10;
        trainNodes = new ArrayList<DataNode>();
        mInputCount = inputCount;
        mHiddenCount = hiddenCount;
        mOutputCount = outputCount;
        mInputNodes = new ArrayList<>();
        mHiddenNodes = new ArrayList<>();
        mOutputNodes = new ArrayList<>();
        mInputHiddenWeight = new double[inputCount][hiddenCount];
        mHiddenOutputWeight = new double[mHiddenCount][mOutputCount];
    }

    /**
     * Update the weights
     * the gradient of each weight is equal to the output value of the previous layer node connected to it multiply the value of the back propagation of the next layer connected to it
     */
    private void updateWeights(double eta)
    {
        // Update the weight matrix from the input layer to the hidden layer
        for (int i = 0; i < mInputCount; i++)
            for (int j = 0; j < mHiddenCount; j++)
                mInputHiddenWeight[i][j] -= eta
                        * mInputNodes.get(i).getForwardOutputValue()
                        * mHiddenNodes.get(j).getBackwardOutputValue();
        // Update the weight matrix from the hidden layer to the output layer
        for (int i = 0; i < mHiddenCount; i++)
            for (int j = 0; j < mOutputCount; j++)
                mHiddenOutputWeight[i][j] -= eta
                        * mHiddenNodes.get(i).getForwardOutputValue()
                        * mOutputNodes.get(j).getBackwardOutputValue();
    }

    /**
     * forward propagation
     */
    private void forward(List<Double> list)
    {
        //input layer
        for (int k = 0; k < list.size(); k++)
            mInputNodes.get(k).setForwardInputValue(list.get(k));
        //hidden layer
        for (int j = 0; j < mHiddenCount; j++)
        {
            double temp = 0;
            for (int k = 0; k < mInputCount; k++)
                temp += mInputHiddenWeight[k][j]
                        * mInputNodes.get(k).getForwardOutputValue();
            mHiddenNodes.get(j).setForwardInputValue(temp);
        }
        //output layer
        for (int j = 0; j < mOutputCount; j++)
        {
            double temp = 0;
            for (int k = 0; k < mHiddenCount; k++)
                temp += mHiddenOutputWeight[k][j]
                        * mHiddenNodes.get(k).getForwardOutputValue();
            mOutputNodes.get(j).setForwardInputValue(temp);
        }
    }

    /**
     * back propagation
     */
    private void backward(int category)
    {
        //output layer
        for (int j = 0; j < mOutputCount; j++)
        {
            // The output layer calculates the error value and propagates the error value back
            // -1 means not belonging, 1 means belonging
            double result = (j == category) ? 1 : -1;
            mOutputNodes.get(j).setBackwardInputValue(
                    mOutputNodes.get(j).getForwardOutputValue() - result);
        }
        //hidden layer
        for (int j = 0; j < mHiddenCount; j++)
        {
            double temp = 0;
            for (int k = 0; k < mOutputCount; k++)
                temp += mHiddenOutputWeight[j][k]
                        * mOutputNodes.get(k).getBackwardOutputValue();
            mHiddenNodes.get(j).setBackwardInputValue(temp);
        }
    }

    /**
     * @param eta learning rate
     * @param n   learning times
     */
    public void train(double eta, int n, CLIProgressPrinter... cliProgressPrinter)
    {
        initialize();
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < trainNodes.size(); j++)
            {
                forward(trainNodes.get(j).getAttrList());
                backward(trainNodes.get(j).getCategory());
                updateWeights(eta);
            }
            if (cliProgressPrinter != null)
                cliProgressPrinter[0].print(i);
        }
        if (cliProgressPrinter != null)
        {
            cliProgressPrinter[0].print(n);
            System.out.println();
        }
    }

    /**
     * initialization
     */
    private void initialize()
    {
        mInputNodes.clear();
        mHiddenNodes.clear();
        mOutputNodes.clear();
        for (int i = 0; i < mInputCount; i++)
            mInputNodes.add(new NetworkNode(NetworkNode.TYPE_INPUT));
        for (int i = 0; i < mHiddenCount; i++)
            mHiddenNodes.add(new NetworkNode(NetworkNode.TYPE_HIDDEN));
        for (int i = 0; i < mOutputCount; i++)
            mOutputNodes.add(new NetworkNode(NetworkNode.TYPE_OUTPUT));
        for (int i = 0; i < mInputCount; i++)
            for (int j = 0; j < mHiddenCount; j++)
                mInputHiddenWeight[i][j] = (double) (Math.random() * 0.1);
        for (int i = 0; i < mHiddenCount; i++)
            for (int j = 0; j < mOutputCount; j++)
                mHiddenOutputWeight[i][j] = (double) (Math.random() * 0.1);
    }

    /**
     * @param dn test data node
     * @return category index of test data
     */
    public int test(DataNode dn)
    {
        forward(dn.getAttrList());
        double result = Double.MAX_VALUE;
        int nodeIndex = 0;
        // Record and return the index of the node whose forward output value is closest to 1 in the output layer
        for (int i = 0; i < mOutputCount; i++)
            if ((1 - mOutputNodes.get(i).getForwardOutputValue()) < result)
            {
                result = 1 - mOutputNodes.get(i).getForwardOutputValue();
                nodeIndex = i;
            }
        return nodeIndex;
    }
}
