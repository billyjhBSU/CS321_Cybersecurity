package cs321.btree;

import java.io.IOException;
import java.io.PrintWriter;

public class BTree implements BTreeInterface
{

    private int degree;
    private long size;
    private long nodes;
    private BTreeNode root;

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
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public void insert(TreeObject obj) throws IOException {
//        root =
    }

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
    }

}

