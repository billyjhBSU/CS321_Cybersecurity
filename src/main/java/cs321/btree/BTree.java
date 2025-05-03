package cs321.btree;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class BTree implements BTreeInterface {
    private int degree;
    private BTreeNode root;
    private String filename;
    private int nodeSize;
    private int numNodes;
    private ByteBuffer buffer;
    private FileChannel fileChannel;

    private final int METADATA_SIZE =
                 Long.BYTES  // Root node offset
            + Integer.BYTES; // Degree (t)

    private long nextDiskAddress = METADATA_SIZE;
    private long rootAddress     = METADATA_SIZE;

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
        this.nodeSize = getNodeSize(degree);
        this.numNodes = 1; // Starts with root only

        buffer = ByteBuffer.allocateDirect(nodeSize);

        // Initialize file storage
        try {
            File file = new File(filename);
            if (!file.exists()) {
                file.createNewFile();
                RandomAccessFile raf = new RandomAccessFile(filename, "rw");
                this.fileChannel = raf.getChannel();

                // Create root node
                root = new BTreeNode(true); // Empty leaf node
                rootAddress = root.address;

                writeMetaData();
                diskWrite(root);
            }
            else {
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
     * Calculates the optimal degree for a BTree with given disk block size based on the size of a {@code BTreeNode}.
     * The math for doing so can be explained as follows:
     * <br><br>
     * Let {@code blockSize} = {@code s}. We need to find the optimal degree {@code t} such that
     * {@link #getNodeSize(int degree)} <= {@code blockSize}, where {@code t == degree}. Given:
     * <ul>
     *     <li> {@code TreeObject.BYTES} = 12</li>
     *     <li> {@code Integer.BYTES} = 4</li>
     *     <li> {@code Long.BYTES} = 8</li>
     * </ul>
     * We substitute:
     * <br>
     * {@code getNodeSize(t)} = 4 + (2{@code t} - 1) * 12 + 2{@code t} * 8 + 1 = 40{@code t} - 7.
     * <br>
     * We then set this less than or equal to {@code s}:
     * <br>
     * 40{@code t} - 7 <= {@code s}.
     * <br>
     * This gives us:
     * {@code s} = floor(({@code s} + 7)/40)
     *
     * @param blockSize
     * @return
     */
    public static int calculateOptimalMinimumDegree(int blockSize) {
        return (int) Math.floor((blockSize + 7)/40.0);
    }

    /**
     * Read the metadata from the data file.
     * @throws IOException
     */
    public void readMetaData() throws IOException {
        fileChannel.position(0);

        ByteBuffer tmpBuffer = ByteBuffer.allocateDirect(METADATA_SIZE);

        tmpBuffer.clear();
        fileChannel.read(tmpBuffer);

        tmpBuffer.flip();
        rootAddress = tmpBuffer.getLong();
        degree      = tmpBuffer.getInt();
    }


    /**
     * Write the metadata to the data file.
     * @throws IOException
     */
    public void writeMetaData() throws IOException {
        fileChannel.position(0);

        ByteBuffer tmpBuffer = ByteBuffer.allocateDirect(METADATA_SIZE);

        tmpBuffer.clear();
        tmpBuffer.putLong(rootAddress);
        tmpBuffer.putInt(degree);

        tmpBuffer.flip();
        fileChannel.write(tmpBuffer);
    }

    /**
     * Reads a {@code BTreeNode} from the disk and returns a {@code BTreeNode} object built from the data
     * @param diskAddress the address offset, in bytes, for the node in the data file
     * @return the {@code BTreeNode} object
     * @throws IOException
     */
    private BTreeNode diskRead(long diskAddress) throws IOException {
        if (diskAddress == 0) {
            return null;
        }

        // Set read position for fileChannel
        fileChannel.position(diskAddress);
        buffer.clear();

        // Read data into buffer
        fileChannel.read(buffer);
        buffer.flip();

        // Read numKeys
        int numKeys = buffer.getInt();

        int maxKeys = 2 * degree - 1;
        TreeObject[] objects = new TreeObject[maxKeys];
        for (int i = 0; i < maxKeys; i++) {
            // Each TreeObject is 72 bytes, so get first 64 to obtain key value
            byte[] keyBytes = new byte[TreeObject.BYTES - Long.BYTES];
            buffer.get(keyBytes);

            // Convert byte array to string
            String key = new String(keyBytes).trim();

            // Remaining 8 bytes in this TreeObject are the frequency
            long frequency = buffer.getLong();

            // Create TreeObject from key and frequency only if another key exists
            if (i < numKeys) {
                objects[i] = new TreeObject(key, frequency);
            }
        }

        // Read leaf flag
        boolean leaf = (buffer.get() == 1);

        // Create BTreeNode before reading children to avoid having to do two more loops
        try {
            BTreeNode node = new BTreeNode(objects, leaf);

            // Set each child pointer value by reading (2 * degree) longs from buffer
            int maxChildren = 2 * degree;
            for (int i = 0; i < maxChildren; i++) {
                long address = buffer.getLong();
                node.setChild(i, address);
            }

            // Assign numKeys and address value to finish populating node object
            node.numKeys = numKeys;
            node.address = diskAddress;

            return node;
        } catch (BTreeException bte) {
            System.err.println(bte.getMessage());
            return null;
        }
    }

    /**
     * Writes a {@code BTreeNode} to the disk at the specified disk offset address in the {@code BTreeNode} object.
     * @param node the {@code BTreeNode} to write
     * @return the disk address of the written node
     * @throws IOException
     */
    public long diskWrite(BTreeNode node) throws IOException {
        fileChannel.position(node.address);
        buffer.clear();

        // Write number of keys in node
        buffer.putInt(node.numKeys);

        int maxKeys = 2 * degree - 1;
        for (int i = 0; i < maxKeys; i++) {
            if (i > node.numKeys || node.getKeyAt(i) == null) { // If key doesn't exist
                // Write placeholder key (zero-filled)
                buffer.put(new byte[64]); // Value
                buffer.putLong(0L); // Frequency
            }
            else {
                // Write actual key
                byte[] keyBytes = node.getKeyAt(i).getKey().getBytes();
                keyBytes = Arrays.copyOf(keyBytes, 64);  // Pad/truncate key array to 64 bytes
                buffer.put(keyBytes);
                buffer.putLong(node.getKeyAt(i).getCount());
            }
        }

        // Write leaf flag (1 byte)
        buffer.put((byte) (node.leaf ? 1 : 0));

        // Write children
        int maxChildren = degree * 2;
        for (int i = 0; i < maxChildren; i++) {
            buffer.putLong(node.getChild(i));
        }

        buffer.flip();
        fileChannel.write(buffer);

        return node.address;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize() {
        try {
            return getSizeFromNode(root);
        } catch (IOException e) {
            throw new RuntimeException("Failed to compute BTree size", e);
        }
    }

    /**
     * Utility method used by {@code getSize()} to find the size of the BTree. Performs a recursive post-order
     * traversal of the BTree to sum up the number of keys contained in each node. If called on the root node of the
     * BTree, returns the BTree's height.
     * @return the number of keys in the BTree rooted at {@code node}
     * @throws IOException if an issue occurs with disk read
     */
    private long getSizeFromNode(BTreeNode node) throws IOException {
        if (node == null) {
            return 0;
        }

        long total = node.numKeys;

        if (!node.isLeaf()) {
            for (int i = 0; i <= node.numKeys; i++) {
                long childAddr = node.getChild(i);
                if (childAddr != 0) {
                    BTreeNode child = diskRead(childAddr);
                    total += getSizeFromNode(child);
                }
            }
        }

        return total;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDegree() {
        return this.degree;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getNumberOfNodes() {
        return this.numNodes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getHeight() {
        try {
            return getHeightFromNode(this.root);
        } catch (IOException e) {
            return -1; //fixme throw RuntimeException?
        }
    }

    /**
     * Utility method used by {@code getHeight()}. Recursively walks down the leftmost child pointers of nodes in the
     * BTree starting at {@code node} until a leaf is reached. If called on the root node of the BTree, returns the
     * BTree's height.
     * @param node the node from which to start the walk
     * @return the height of the BTree rooted at {@code node}
     * @throws IOException if an issue occurs with disk read
     */
    private int getHeightFromNode(BTreeNode node) throws IOException {
        if (node == null || node.isLeaf()) {
            return 0;
        }

        BTreeNode leftChild = diskRead(node.getChild(0));
        return 1 + getHeightFromNode(leftChild);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dumpToFile(PrintWriter out) throws IOException {
        recursiveDump(root, out);
    }

    /**
     * In-order traversal at a given node in a BTree then writes
     * the key and frequency using PrintWriter
     * @param node current node
     * @param out PrintWriter to write key and count
     * @throws IOException
     * Note from Ado: if editing look at: https://www.geeksforgeeks.org/tree-traversals-inorder-preorder-and-postorder/
     */
    public void recursiveDump(BTreeNode node, PrintWriter out) throws IOException{
        if(node == null){
            return;
        }
        for(int i = 0; i < node.numKeys; i++){
            if(!node.isLeaf()){
                BTreeNode child = diskRead(node.getChild(i));
                recursiveDump(child, out);
            }
            TreeObject obj = node.getKeyAt(i);
            if(obj != null){
                out.println(obj.getKey() + " " + obj.getCount());
            }
        }
        if(!node.isLeaf()){
            BTreeNode child = diskRead(node.getChild(node.numKeys));
            recursiveDump(child, out);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void dumpToDatabase(String dbName, String tableName) throws IOException {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbName))
        {
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + tableName + " (key TEXT PRIMARY KEY, count INTEGER)");
            recursiveDumpToDatabase(root, statement, tableName);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void recursiveDumpToDatabase(BTreeNode node, Statement statement, String tableName) throws SQLException, IOException
    {
        if(node == null){
            return;
        }
        for(int i = 0; i < node.numKeys; i++){
            if(!node.isLeaf()){
                BTreeNode child = diskRead(node.getChild(i));
                recursiveDumpToDatabase(child, statement, tableName);
            }
            TreeObject obj = node.getKeyAt(i);
            if(obj != null){
                String value = "('" + obj.getKey() + "', " + obj.getCount() + ")";
                statement.executeUpdate("INSERT OR IGNORE INTO " + tableName + " (key, count) VALUES ('" + obj.getKey() + "', " + obj.getCount() + ")");
            }
        }
        if(!node.isLeaf()){
            BTreeNode child = diskRead(node.getChild(node.numKeys));
            recursiveDumpToDatabase(child, statement, tableName);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TreeObject search(String key) throws IOException {
        return searchFromNode(key, root);
    }

    /**
     * Searches for the given key in the BTree rooted at {@code node}.
     * @param node the node from which to begin the search
     * @param key the key value to search for
     * @return the {@code TreeObject} in this BTree containing {@code key}
     * @throws IOException if issue occurs with disk read
     */
    private TreeObject searchFromNode(String key, BTreeNode node) throws IOException {
        int i = 0;
        while (i < node.numKeys && key.compareTo(node.getKeyAt(i).getKey()) > 0) {
            i += 1;
        }
        if (i < node.numKeys && key.compareTo(node.getKeyAt(i).getKey()) == 0) {
            return node.getKeyAt(i);
        }
        else if (node.isLeaf()) {
            return null;
        }
        else {
            return searchFromNode(key, diskRead(node.getChild(i)));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String key) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert(TreeObject obj) throws IOException {
        BTreeNode oldRoot = root;
        if (oldRoot.isFull()) {
            BTreeNode newRoot = splitRoot(); // splitRoot() Handles disk/metadata updating
            insertNonfull(newRoot, obj);
        }
        else {
            insertNonfull(oldRoot, obj);
        }
    }

    private void splitChild(BTreeNode parent, int index) throws IOException {
        try {
            BTreeNode fullChild = diskRead(parent.getChild(index)); // y
            BTreeNode newNode = new BTreeNode(fullChild.isLeaf()); // z
            newNode.numKeys = degree - 1;

            //z gets y's greater half of keys
            for (int j = 0; j <= degree - 2; j++) {
                newNode.setKey(j, fullChild.getKeyAt(j + degree));
            }

            if (!fullChild.isLeaf()) {
                for (int j = 0; j <= degree - 1; j++) {
                    newNode.setChild(j, fullChild.getChild(j + degree));
                }
            }

            fullChild.numKeys = degree - 1;

            for (int j = parent.numKeys; j >= index + 1; j--) {
                parent.setChild(j + 1, parent.getChild(j));
            }
            parent.setChild(index + 1, newNode.address);

            for (int j = parent.numKeys - 1; j >= index; j--) {
                parent.setKey(j + 1, parent.getKeyAt(j));
            }
            parent.setKey(index, fullChild.getKeyAt(degree - 1));
            parent.numKeys += 1;

            this.numNodes += 1;
            diskWrite(fullChild);
            diskWrite(newNode);
            diskWrite(parent);
        } catch (BTreeException bte) {
            System.err.println(bte.getMessage());
        }
    }

    private BTreeNode splitRoot() throws IOException {
        try {
            BTreeNode oldRoot = root;
            BTreeNode newRoot = new BTreeNode(false);
            newRoot.setChild(0, oldRoot.address);
            this.root = newRoot;
            this.rootAddress = newRoot.address;

            splitChild(newRoot, 0);

            this.numNodes += 1;
            diskWrite(newRoot); // Return value not needed as root address was updated already
            writeMetaData(); // Update metadata since root address changed

            return newRoot;
        } catch (BTreeException bte) {
            System.err.println(bte.getMessage());
            return null;
        }
    }

    private void insertNonfull(BTreeNode node, TreeObject obj) throws IOException {
        int i = node.numKeys - 1;

        if (node.isLeaf()) {
            while (i >= 0 && obj.compareTo(node.getKeyAt(i)) < 0) {
                node.setKey(i + 1, node.getKeyAt(i));
                i -= 1;
            }

            // If key is a duplicate, increment its count instead of inserting
            if (i >= 0 && obj.compareTo(node.getKeyAt(i)) == 0) {
                node.getKeyAt(i).incCount();
                diskWrite(node);
                return;
            }

            node.setKey(i + 1, obj);
            node.numKeys += 1;
            diskWrite(node);
        }
        else {
            while (i >= 0 && obj.compareTo(node.getKeyAt(i)) < 0) {
                i -= 1;
            }

            // If key is a duplicate, increment its count instead of inserting
            if (i >= 0 && obj.compareTo(node.getKeyAt(i)) == 0) {
                node.getKeyAt(i).incCount();
                diskWrite(node);
                return;
            }

            i += 1;
            BTreeNode child = diskRead(node.getChild(i));

            // Before splitting check if there is a duplicate within child
            // If duplicate found, increment count instead of inserting
            for (int j = 0; j < child.numKeys; j++) {
                if (obj.compareTo(child.getKeyAt(j)) == 0) {
                    child.getKeyAt(j).incCount();
                    diskWrite(child);
                    return;
                }
            }

            if (child.isFull()) { // If child is full, split it
                splitChild(node, i);
                if (obj.compareTo(node.getKeyAt(i)) > 0) {
                    i += 1;
                    child = diskRead(node.getChild(i));
                }
            }
            insertNonfull(diskRead(node.getChild(i)), obj);
        }
    }

    /**
     * Gets the keys of this BTree in sorted order.
     * @return a {@code String} array containing all the keys in this BTree in sorted order
     */
    public String[] getSortedKeyArray() {
        List<String> sortedKeys = new ArrayList<>();
        try {
            inorderRetrieveKeys(root, sortedKeys);
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve sorted key array", e);
        }
        return sortedKeys.toArray(new String[0]);
    }

    /**
     * Performs a recursive inorder walk on the BTree starting from {@code node} and stores the result in the list
     * {@code result}.
     * @param node the node from which to start the inorder walk
     * @param result the list in which to store the BTree keys in order
     * @throws IOException if issue occurs with disk read
     */
    private void inorderRetrieveKeys(BTreeNode node, List<String> result) throws IOException {
        if (node == null) {
            return;
        }

        // Traverse each child recursively
        for (int i = 0; i < node.numKeys; i++) {
            if (!node.isLeaf()) {
                long childAddress = node.getChild(i);
                if (childAddress != 0) {
                    BTreeNode child = diskRead(childAddress);
                    inorderRetrieveKeys(child, result);
                }
            }

            TreeObject key = node.getKeyAt(i);
            if (key != null) {
                result.add(key.getKey());
            }
        }

        // Traverse Final child
        if (!node.isLeaf()) {
            long lastChildAddress = node.getChild(node.numKeys);
            if (lastChildAddress != 0) {
                BTreeNode child = diskRead(lastChildAddress);
                inorderRetrieveKeys(child, result);
            }
        }
    }

    public void close() throws IOException {
        if (fileChannel != null && fileChannel.isOpen()) {
            fileChannel.close();
        }
    }

    /**
     * Gets the size (in bytes) of a {@code BTreeNode} in a {@code BTree} with given degree.
     * @param degree the degree of the {@code BTree}
     * @return the size, in bytes, of a node
     */
    public static int getNodeSize(int degree) {
        return Integer.BYTES + // numKeys
                (2 * degree - 1) * TreeObject.BYTES + // Keys
                2 * degree * Long.BYTES + // Child pointers
                1; // Leaf flag
    }

    public class BTreeNode {

        private TreeObject[] keys;
        private int numKeys;
        private boolean leaf;
        private long[] children;
        private long address; // Disk offset (bytes)

        /**
         * TODO
         * @param keys
         * @param leaf
         */
        public BTreeNode(TreeObject[] keys, boolean leaf) throws BTreeException {
            if (keys.length != 2 * degree - 1) {
                throw new BTreeException("keys array length does not match degree array length");
            }

            this.keys = keys;
            this.leaf = leaf;
            this.children = new long[2 * degree];

            // Calculate numKeys as the number of non-null entries in keys array
            int keyCount = 0;
            for (TreeObject key : keys) {
                if (key == null) {
                    break; // Stop on first null
                }
                keyCount += 1;
            }
            this.numKeys = keyCount;

            // Set disk address and update pointer
            address = nextDiskAddress;
            nextDiskAddress += nodeSize;
        }

        /**
         * Creates a new {@code BTreeNode} with an empty {@code keys} array and given leaf status.
         * @param leaf whether the created node is a leaf (has no children)
         */
        public BTreeNode(boolean leaf) throws BTreeException {
            this(new TreeObject[2 * degree - 1], leaf);
        }

        /**
         * Gets whether this node is a leaf.
         * @return true if this node is a leaf, false otherwise
         */
        public boolean isLeaf() {
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
        public TreeObject getKeyAt(int i) {
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

