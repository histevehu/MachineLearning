package util;

public class CLIProgressPrinter implements ICLIProgressPrinter
{
    private double total;
    private double value;
    private int width = 20;
    private boolean monopolyPrint = true;
    private String content = "";
    private int lastFinishProgressLen = -1;

    public double getTotal()
    {
        return total;
    }

    public double getValue()
    {
        return value;
    }

    public void setValue(double value)
    {
        this.value = value;
    }

    public int getWidth()
    {
        return width;
    }

    public void setWidth(int width)
    {
        this.width = width;
    }

    public boolean isMonopolyPrint()
    {
        return monopolyPrint;
    }

    public void setMonopolyPrint(boolean monopolyPrint)
    {
        this.monopolyPrint = monopolyPrint;
    }

    public CLIProgressPrinter(double total)
    {
        this.total = total;
    }

    @Override
    public void print(double value)
    {
        int finishProgressLen = (int) Math.floor(value / total * width);
        if (finishProgressLen != lastFinishProgressLen)
        {
            lastFinishProgressLen = finishProgressLen;
            for (int i = 0; i < content.length(); i++)
            {
                System.out.print("\b");
            }
            String newContent = "[";
            for (int i = 0; i < finishProgressLen; i++)
            {
                newContent += "■";
            }
            for (int i = 0; i < width - finishProgressLen; i++)
            {
                newContent += " ";
            }
            newContent += "] " + value + "/" + total;
            content = newContent;
            System.out.print(newContent);
        }

    }
}
