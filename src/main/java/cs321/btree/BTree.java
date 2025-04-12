package cs321.btree;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class BTree implements BTreeInterface {
    private final  int METADATA_SIZE = Long.BYTES;
    private long nextDiskAddress = METADATA_SIZE;

    private int degree;
    private BTreeNode root;
    private String filename;
    private long nodeSize;
    private int numNodes;
    private ByteBuffer buffer;
    private FileChannel fileChannel;

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

        // Initialize file storage
        try {
            File file = new File(filename);
            if (!file.exists()) {
                file.createNewFile();
                RandomAccessFile raf = new RandomAccessFile(filename, "rw");
                this.fileChannel = raf.getChannel();
                writeMetaData();
//                root = new BTreeNode(true); TODO need this?
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

        // Read each value from the node
        int loopMax = this.numNodes; //fixme upper condition might be wrong
        TreeObject[] keys = new TreeObject[loopMax];
        for (int i = 0; i < this.nodeSize; i++) {
            long value = buffer.getLong();
            long freq = buffer.getLong(); // Read frequency

            String strValue = "" + value; // fixme this is probably wrong
            keys[i] = new TreeObject(strValue, freq);
        }

        // Read next byte to get leaf flag
        byte flag = buffer.get();
        boolean leaf = (flag == 1);



    }

    /**
     * Writes a {@code BTreeNode} to the disk at the specified disk offset address in the {@code BTreeNode} object.
     * @param node the {@code BTreeNode} to write
     * @throws IOException
     */
    public void diskWrite(BTreeNode node) throws IOException {

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

    private class BTreeNode {

        private TreeObject[] keys;
        private int numKeys;
        private boolean leaf;
        private long[] children;
        private long address; // Disk offset (bytes)

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
         * Gets the child pointer at the provided index.
         * @param i the index
         * @return the child pointer at index {@code i}
         */
        public long getChild(int i) {
            return children[i];
        }
    }
}
