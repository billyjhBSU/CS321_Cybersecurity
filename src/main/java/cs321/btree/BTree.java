package cs321.btree;

import java.io.IOException;
import java.io.PrintWriter;

public class BTree implements BTreeInterface {

    private int degree;
    private long size;
    private long nodes;
    private BTreeNode root;
    private long offset = 0;

    public BTree(int degree) throws BTreeException {
        if (degree < 2) {
            throw new BTreeException("Degree must be greater then or equal to 2.");
        }
        this.degree = degree;
        this.nodes = 0;
        this.size = 0;
        this.root = null;
    }

//    public BTree(int degree, String fileName) throws BTreeException {
//        this(degree);
//    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public int getDegree() {
        return degree;
    }

    @Override
    public long getNumberOfNodes() {
        return nodes;
    }

    @Override
    public int getHeight() {
        if (root == null) {
            return 0;
        }
        return 0;
    }

    @Override
    public void insert(TreeObject obj) throws IOException {
        /*
        r == T.root
        if r.n == 2t-1
            s = BTreeSplitRoot()
            BtreeInsertNonfull(s, k)
        else
            BtreeInsertNonfull(r, k)
         */
    }

    private void BtreeSplitChild(BTreeNode rootNode, int index) {
        BTreeNode y = DiskRead(rootNode.children[index]);
        BTreeNode z = new BTreeNode(new TreeObject[2 * degree - 1], rootNode.leaf()); // new node
        z.leaf = y.leaf;
        z.numKeys = degree - 1;
        //z gets y's greater half of keys
        for (int j = 0; j < degree - 1; j++) {
            z.keys[j] = z.keys[j + degree];
        }
        if (!y.leaf()) {
            for (int j = 0; j < degree; j++) {
                z.children[j] = z.children[j + degree];
            }
        }
        y.numKeys = degree - 1;
        for (int j = rootNode.numKeys; j > index; j--) {
            rootNode.children[j + 1] = rootNode.children[j];
        }
        rootNode.keys[index] = y.keys[degree - 1];
        rootNode.numKeys++;
        DiskWrite(rootNode);
        DiskWrite(y);
        DiskWrite(z);
    }

    private BTreeNode BtreeSplitRoot() {
        BTreeNode s = new BTreeNode(new TreeObject[2 * degree - 1], false);
        s.leaf = false;
        s.numKeys = 0;
        s.children[0] = offset;
        offset = DiskWrite(s);
        BtreeSplitChild(s, 0);
        return s;
}
    /*
    BtreeInsertNonfull()
        i = n-1
        if x.leaf
            while i >= 0 and k < x.key[i]
                x.key[i+1] = x.k[i]
                i = i-1
            x.k[i+1] = k
            x.n = x.n+1
            DiskWrite(x)
        else
            while i >= 0 and k < k.key[i]
                i = i-1
            i = i+1
            DiskRead(x.c[i])
            if x.c[i].n == 2t-1
                BtreeSplitChild(x, i)
                if k > x.key[i]
                    i = i+1
                    DiskRead(x.c[i])
            BTreeInsertNonFull(x.c[i], k)

     */
    @Override
    public void dumpToFile(PrintWriter out) throws IOException {

    }

    @Override
    public void dumpToDatabase(String dbName, String tableName) throws IOException {

    }

    @Override
    public TreeObject search(String key) throws IOException {
        return null;
    }

    @Override
    public void delete(String key) {

    }

    private class BTreeNode {
        private TreeObject[] keys;
        private int numKeys;
        private boolean leaf;
        private long[] children;
        //public static final int BYTES = Integer.BYTES + TreeObject.BYTES + 1 + 3 * Long.BYTES;
        public BTreeNode(TreeObject[] keys, boolean leaf) {
            this.keys = keys;
            this.leaf = leaf;
            children = null;
            numKeys = 0;
        }

        public boolean leaf() {
            return leaf;
        }
    }

}

