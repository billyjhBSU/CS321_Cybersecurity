package cs321.btree;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class BTree implements BTreeInterface {
    private int degree;
    private BTreeNode root;
    private String filename;
    private long nodeSize;
    private int numNodes;
    private ByteBuffer buffer;
    private FileChannel fileChannel;
    private long offset;

    private final  int METADATA_SIZE = Long.BYTES;
    private long nextDiskAddress = METADATA_SIZE;


    private long rootAddress = METADATA_SIZE;

    /**
     * Creates a new {@code BTree} with default minimum degree of 2 and specified filename.
     * @param filename the name of the file where the {@code BTree} will be stored
     */
    public BTree(String filename) throws BTreeException {
        this(2, filename);
    }

    /**
     * Creates a new {@code BTree} with specified degree and filename.
     * @param degree the minimum degree of the {@code BTree}
     * @param filename the name of the file where the {@code BTree} will be stored
     */
    public BTree(int degree, String filename) throws BTreeException {
        if (degree < 2) {
            throw new BTreeException("Degree must be at least 2.");
        }

        this.degree = degree;
        this.filename = filename;
        this.nodeSize = new BTreeNode(true).BYTES; // Create dummy BTreeNode object to get non-static size
        this.numNodes = 1; // Starts with root only
        this.offset = -1;

        // Initialize file storage
        try {
            File file = new File(filename);
            if (!file.exists()) {
                file.createNewFile();
                RandomAccessFile raf = new RandomAccessFile(filename, "rw");
                this.fileChannel = raf.getChannel();
                writeMetaData();
                root = new BTreeNode(true); //TODO need this?
            } else {
                RandomAccessFile raf = new RandomAccessFile(filename, "rw");
                this.fileChannel = raf.getChannel();
                readMetaData();
                this.root = diskRead(rootAddress);
            }
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }

    }

    /**
     * Read the metadata from the data file.
     * @throws IOException
     */
    public void readMetaData() throws IOException {
        fileChannel.position(0);

        ByteBuffer tmpbuffer = ByteBuffer.allocateDirect(METADATA_SIZE);

        tmpbuffer.clear();
        fileChannel.read(tmpbuffer);

        tmpbuffer.flip();
        rootAddress = tmpbuffer.getLong();
    }


    /**
     * Write the metadata to the data file.
     * @throws IOException
     */
    public void writeMetaData() throws IOException {
        fileChannel.position(0);

        ByteBuffer tmpbuffer = ByteBuffer.allocateDirect(METADATA_SIZE);

        tmpbuffer.clear();
        tmpbuffer.putLong(rootAddress);

        tmpbuffer.flip();
        fileChannel.write(tmpbuffer);
    }

    /**
     * Reads a {@code BTreeNode} from the disk and returns a {@code BTreeNode} object built from the data
     * @param diskAddress the address offset, in bytes, for the node in the data file
     * @return the {@code BTreeNode} object
     * @throws IOException
     */
    public BTreeNode diskRead(long diskAddress) throws IOException {
        if (diskAddress == 0) {
            return null;
        }

        // Set start address to read from
        fileChannel.position(diskAddress);
        buffer.clear();

        // Read data into buffer
        fileChannel.read(buffer);
        buffer.clear();

        int n = buffer.getInt(); // TODO do we need this?

        // Read each value from the node
        int maxNumNodes = this.numNodes; // fixme upper condition might be wrong
        TreeObject[] keys = new TreeObject[maxNumNodes];
        for (int i = 0; i < maxNumNodes; i++) {
            long value = buffer.getLong();
            long freq = buffer.getLong(); // Read frequency

            String strValue = "" + value;
            keys[i] = new TreeObject(strValue, freq);
        }

        // Read next byte to get leaf flag
        byte flag = buffer.get();
        boolean leaf = (flag == 1);

        // Read data for each child pointer
        int maxNumChildren = 2 * degree;
        long[] children = new long[2 * degree];
        for (int i = 0; i < 2 * degree; i++) {
            children[i] = buffer.getLong();
        }

        BTreeNode node = new BTreeNode(keys, leaf);
        node.n = n;
        for (int i = 0; i < maxNumChildren; i++) {
            node.setChild(i, children[i]);
        }
        node.address = diskAddress;

        return node;
    }

    /**
     * Writes a {@code BTreeNode} to the disk at the specified disk offset address in the {@code BTreeNode} object.
     * @param node the {@code BTreeNode} to write
     * @throws IOException
     */
    public long diskWrite(BTreeNode node) throws IOException {
        fileChannel.position(node.address);
        buffer.clear();

        buffer.putInt(node.n);



        int numKeys = node.numKeys;
        for (int i = 0; i < numKeys; i++) {
            buffer.putLong(Long.parseLong(node.getKey(i).getKey())); // Write value
            buffer.putLong(node.getKey(i).getCount()); // Write frequency
        }

        //TODO refactor this
        if (node.leaf()) {
            buffer.put((byte) 1);
        }
        else {
            buffer.put((byte) 0);
        }

        int numChildren = degree * 2;
        for (int i = 0; i < numChildren; i++) {
            buffer.putLong(node.getChild(i));
        }

        buffer.flip();
        fileChannel.write(buffer);

        return 0; //fixme
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize() {
        return 0; //TODO
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDegree() {
        return 0; //TODO
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getNumberOfNodes() {
        return 0; //TODO
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getHeight() {
        return 0; //TODO
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert(TreeObject obj) throws IOException {
        BTreeNode rootNode = root;
        if (rootNode.numKeys == 2*degree-1) {
            BTreeNode newRoot = BtreeSplitRoot();
            BtreeInsertNonfull(newRoot, obj);
        }
        else {
            BtreeInsertNonfull(root, obj);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dumpToFile(PrintWriter out) throws IOException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dumpToDatabase(String dbName, String tableName) throws IOException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TreeObject search(String key) throws IOException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String key) {

    }

    private void BtreeSplitChild(BTreeNode rootNode, int index) throws IOException {
        BTreeNode y = diskRead(rootNode.children[index]);
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
        diskWrite(rootNode);
        diskWrite(y);
        diskWrite(z);
    }

    private BTreeNode BtreeSplitRoot() throws IOException {
        BTreeNode s = new BTreeNode(new TreeObject[2 * degree - 1], false);
        s.leaf = false;
        s.numKeys = 0;
        s.children[0] = offset;
        //offset = diskWrite(s);
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

    private void BtreeInsertNonfull(BTreeNode node, TreeObject obj) throws IOException {
        int i =node.numKeys -1;
        if (node.leaf()) {
            while (i >= 0 && obj.compareTo(node.keys[i]) < 0){
                node.keys[i+1] = node.keys[i];
                i--;
            }
            node.keys[i+1] = obj;
            node.numKeys++;
            diskWrite(node);
        } else {
            while (i >= 0 && obj.compareTo(node.keys[i]) < 0) {
                i--;
            }
            i++;
            BTreeNode newNode = diskRead(node.children[i]);
            if (newNode.numKeys == 2 * degree - 1) {
                BtreeSplitChild(node, i);
                if (obj.compareTo(node.keys[i]) > 0) {
                    i++;
                }
            }
            newNode = diskRead(node.children[i]);
            BtreeInsertNonfull(newNode, obj);
        }
    }

    private class BTreeNode {

        private TreeObject[] keys;
        private int numKeys;
        private boolean leaf;
        private long[] children;
        private long address; // Disk offset (bytes)

        private int n;

        /**
         * Calculate the size of a node as stored on disk (in bytes). We will store boolean
         * as 1 byte as its nodeSize is not defined in Java
         */
        final private int BYTES = Integer.BYTES + ((2 * degree - 1) * TreeObject.BYTES) + ((2 * degree) * Long.BYTES) + 1;

        /**
         * TODO
         * @param keys
         * @param leaf
         */
        public BTreeNode(TreeObject[] keys, boolean leaf) {
            //TODO: validate that keys is length (2 * degree - 1)

            this.keys = keys;
            this.leaf = leaf;
            this.numKeys = 0;
            this.children = new long[2 * degree];


            // Set disk address and update pointer
            address = nextDiskAddress;
            nextDiskAddress += nodeSize;

        }

        /**
         * Creates a new {@code BTreeNode} with an empty {@code keys} array and given leaf status.
         * @param leaf whether the created node is a leaf (has no children)
         */
        public BTreeNode(boolean leaf) {
            this(new TreeObject[2 * degree - 1], true);
        }

        /**
         * Gets whether this node is a leaf.
         * @return true if this node is a leaf, false otherwise
         */
        public boolean leaf() {
            return leaf;
        }


        /**
         * Checks if this node is full.
         * @return true if full, false otherwise
         */
        public boolean isFull() {
            return numKeys == 2 * degree - 1;
        }

        /**
         * Gets the key at the given index.
         * @param i the index
         * @return the key at index {@code i}
         */
        public TreeObject getKey(int i) {
            return keys[i];
        }

        /**
         * Sets the key at the given index to {@code key}.
         * @param i the index
         * @param key the value to assign to the given key
         */
        public void setKey(int i, TreeObject key) {
            keys[i] = key;
        }

        /**
         * Sets this node's child pointer at index {@code i} to {@code address}
         * @param i the index
         * @param address the address value to which to assign the child pointer
         */
        public void setChild(int i, long address) {
            children[i] = address;
        }

        /**
         * Gets the child pointer at the provided index.
         * @param i the index
         * @return the child pointer at index {@code i}
         */
        public long getChild(int i) {
            return children[i];
        }
    }


}

