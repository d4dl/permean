package com.d4dl.permean.mesh.positiontree;

import com.d4dl.permean.mesh.Position;

/**
 */
public class AVLTreeRecursive {
    Node root;

    // A utility function to get height of the tree
    int height(Node N) {
        if (N == null)
            return 0;

        return N.height;
    }

    // A utility function to get maximum of two integers
    int max(int a, int b) {
        return (a > b) ? a : b;
    }

    // A utility function to right rotate subtree rooted with y
    // See the diagram given above.
    Node rightRotate(Node y) {
        Node x = y.left;
        Node T2 = x.right;

        // Perform rotation
        x.right = y;
        y.left = T2;

        // Update heights
        y.height = max(height(y.left), height(y.right)) + 1;
        x.height = max(height(x.left), height(x.right)) + 1;

        // Return new root
        return x;
    }

    // A utility function to left rotate subtree rooted with x
    // See the diagram given above.
    Node leftRotate(Node x) {
        Node y = x.right;
        Node T2 = y.left;

        // Perform rotation
        y.left = x;
        x.right = T2;

        //  Update heights
        x.height = max(height(x.left), height(x.right)) + 1;
        y.height = max(height(y.left), height(y.right)) + 1;

        // Return new root
        return y;
    }

    // Get Balance factor of node N
    int getBalance(Node N) {
        if (N == null)
            return 0;

        return height(N.left) - height(N.right);
    }

    public void insert(Position position) {
        root = insert(root, position);
    }

    Node insert(Node node, Position payload) {

        /* 1.  Perform the normal BST insertion */
        if (node == null)
            return (new Node(payload, ""));

        if (payload.compareLatTo(node.payload) < 0)
            node.left = insert(node.left, payload);
        else if (payload.compareLatTo(node.payload) > 0)
            node.right = insert(node.right, payload);
        else // Duplicate payloads not allowed
            return node;

        /* 2. Update height of this ancestor node */
        node.height = 1 + max(height(node.left), height(node.right));

        /* 3. Get the balance factor of this ancestor
              node to check whether this node became
              unbalanced */
        int balance = getBalance(node);

        // If this node becomes unbalanced, then there
        // are 4 cases Left Left Case
        if (balance > 1 && payload.compareLatTo(node.left.payload) < 0)
            return rightRotate(node);

        // Right Right Case
        if (balance < -1 && payload.compareLatTo(node.right.payload) > 0)
            return leftRotate(node);

        // Left Right Case
        if (balance > 1 && payload.compareLatTo(node.left.payload) > 0) {
            node.left = leftRotate(node.left);
            return rightRotate(node);
        }

        // Right Left Case
        if (balance < -1 && payload.compareLatTo(node.right.payload) < 0) {
            node.right = rightRotate(node.right);
            return leftRotate(node);
        }

        /* return the (unchanged) node pointer */
        return node;
    }

    // A utility function to print preorder traversal
    // of the tree.
    // The function also prints height of every node
    void inOrder(Node node) {
        if (node != null) {
            inOrder(node.left);
            System.out.print(node.payload + " ");
            inOrder(node.right);
        }
    }

    // A utility function to print preorder traversal
    // of the tree.
    // The function also prints height of every node
    void preOrder(Node node) {
        if (node != null) {
            System.out.print(node.payload + " ");
            preOrder(node.left);
            preOrder(node.right);
        }
    }
}
