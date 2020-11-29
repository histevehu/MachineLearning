package ann;

import java.util.ArrayList;
import java.util.List;

public class DataNode
{
    private List<Double> attrList;
    private int category;

    public int getCategory()
    {
        return category;
    }

    public void setCategory(int category)
    {
        this.category = category;
    }

    public List<Double> getAttrList()
    {
        return attrList;
    }

    public void addAttr(double value)
    {
        attrList.add(value);
    }

    public DataNode()
    {
        attrList = new ArrayList<>();
    }

}
