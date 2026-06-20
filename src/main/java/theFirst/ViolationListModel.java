package theFirst;

import java.util.Vector;

import javax.swing.AbstractListModel;

public class ViolationListModel<E> extends AbstractListModel<E> {

    /**
     *
     */
    private static final long serialVersionUID = 3192268898876155725L;
    protected Vector<E> list;

    public ViolationListModel(Vector<E> list) {
        this.list = list;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.ListModel#getElementAt(int)
     */
    @Override
    public E getElementAt(int index) {
        try {
            return list.get(index);
        } catch (IndexOutOfBoundsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.ListModel#getSize()
     */
    @Override
    public int getSize() {
        if(list==null){
            return 0;
        }
        return list.size();
    }

}