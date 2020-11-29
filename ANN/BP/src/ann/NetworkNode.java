package ann;

public class NetworkNode
{
    //the types of node in ann
    public static final int TYPE_INPUT = 0;
    public static final int TYPE_HIDDEN = 1;
    public static final int TYPE_OUTPUT = 2;

    private int type;

    public void setType(int type)
    {
        this.type = type;
    }

    // node forward input and output value
    private double mForwardInputValue;
    private double mForwardOutputValue;

    // node backward input and output value
    private double mBackwardInputValue;
    private double mBackwardOutputValue;

    /**
     * @param type the type of node in ann<br>
     *             TYPE_INPUT = 0;<br>
     *             TYPE_HIDDEN = 1;<br>
     *             TYPE_OUTPUT = 2;
     */
    public NetworkNode(int type)
    {
        this.type = type;
    }

    /**
     * sigmoid function, tan-sigmoid and sin-sigmoid are provided here, tan-sigmoid is used by default
     *
     * @param in The weighted sum of the value and weight of each node in the previous layer
     * @return Calculated by the sigmoid function and is the output value of the node
     */
    private double forwardSigmoid(double in)
    {
        switch (type)
        {
            case TYPE_INPUT:
                return in;
            case TYPE_HIDDEN:
            case TYPE_OUTPUT:
                return tanhS(in);
        }
        return 0;
    }

    //log-sigmoid function
    private double logS(double in)
    {
        return (double) (1 / (1 + Math.exp(-in)));
    }

    //log-sigmoid derivative of function
    private double logSDerivative(double in)
    {
        return mForwardOutputValue * (1 - mForwardOutputValue) * in;
    }

    //tan-sigmoid function
    private double tanhS(double in)
    {
        return (double) ((Math.exp(in) - Math.exp(-in)) / (Math.exp(in) + Math
                .exp(-in)));
    }

    //tan-sigmoid derivative of function
    private double tanhSDerivative(double in)
    {
        return (double) ((1 - Math.pow(mForwardOutputValue, 2)) * in);
    }

    /**
     * the derivative of the sigmoid function in backward propagation
     *
     * @param in The weighted sum of the value and weight of each node in the next layer
     * @return Calculated by the sigmoid function and is the output value of the node
     */
    private double backwardPropagate(double in)
    {
        switch (type)
        {
            case TYPE_INPUT:
                return in;
            case TYPE_HIDDEN:
            case TYPE_OUTPUT:
                return tanhSDerivative(in);
        }
        return 0;
    }

    public double getForwardInputValue()
    {
        return mForwardInputValue;
    }

    /**
     * @param mInputValue Set the parameter to the input value of the node in forward propagation, and generate the forward output value of the node through the sigmoid function
     */
    public void setForwardInputValue(double mInputValue)
    {
        this.mForwardInputValue = mInputValue;
        setForwardOutputValue(mInputValue);
    }

    public double getForwardOutputValue()
    {
        return mForwardOutputValue;
    }

    private void setForwardOutputValue(double mInputValue)
    {
        this.mForwardOutputValue = forwardSigmoid(mInputValue);
    }

    public double getBackwardInputValue()
    {
        return mBackwardInputValue;
    }

    /**
     * @param mBackwardInputValue Set the parameter to the input value of the node in back propagation, and generate the backward output value of the node through the sigmoid derivative function
     */
    public void setBackwardInputValue(double mBackwardInputValue)
    {
        this.mBackwardInputValue = mBackwardInputValue;
        setBackwardOutputValue(mBackwardInputValue);
    }

    public double getBackwardOutputValue()
    {
        return mBackwardOutputValue;
    }

    private void setBackwardOutputValue(double input)
    {
        this.mBackwardOutputValue = backwardPropagate(input);
    }

}
