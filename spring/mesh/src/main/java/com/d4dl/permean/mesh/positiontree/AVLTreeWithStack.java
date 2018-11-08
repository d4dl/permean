package com.d4dl.permean.mesh.positiontree;

import com.d4dl.permean.mesh.Position;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 */
public class AVLTreeWithStack {
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

    public Set<Position> findInRectangle(double leftLongitude, double rightLongitude, double topLatitude, double bottomLatidue) {
        Set results = new HashSet();
        findInRectangle(leftLongitude, rightLongitude, topLatitude, bottomLatidue, root, results);
        return results;
    }
    /**
     * Given the positions that make up a rectangle, find the points within.
     * @return
     */
    public void findInRectangle(double bottomφ, double topφ, double leftλ, double rightλ, Node node, Set<Position> results) {
        if (node == null) {
            return;
        }

        if (bottomφ < node.payload.getφ()) {
            findInRectangle(bottomφ, topφ, leftλ, rightλ, node.left, results);
        }

        /* if node's data lies in both ranges, then add */
        if (node.payload.getλ() >=   leftλ &&
            node.payload.getλ() <=  rightλ &&
            node.payload.getφ() <=    topφ &&
            node.payload.getφ() >= bottomφ) {
            results.add(node.payload);
        }

        if (topφ > node.payload.getφ()) {
            findInRectangle(bottomφ, topφ, leftλ, rightλ, node.right, results);
        }
    }


    public void insert(Position payload) {
        if (root == null) {
            root = new Node(payload, "Root");
            return;
        }

        Stack<Node> insertStack = new Stack();
        Node currentNode = root;
        StringBuilder nameBuilder = new StringBuilder();
        int index = 0;

        while (currentNode != null) {
            insertStack.push(currentNode);
            //nameBuilder.append(currentNode.name).append("<-");
            if (payload.compareLatTo(currentNode.payload) < 0) {
                if (currentNode.left == null) {
                    //nameBuilder.append("left(").append(index).append(")");
                    currentNode.left = new Node(payload, nameBuilder.toString());
                    currentNode = null;
                } else {
                    currentNode = currentNode.left;
                }
            } else if (payload.compareLatTo(currentNode.payload) > 0) {
                //nameBuilder.append("right(").append(index).append(")");
                if (currentNode.right == null) {
                    currentNode.right = new Node(payload, nameBuilder.toString());
                    currentNode = null;
                } else {
                    currentNode = currentNode.right;
                }
            } else {// Duplicate payloads not allowed
                throw new IllegalStateException("This node has the same value as another one");
            }
        }

        while (!insertStack.isEmpty()) {
            Node node = insertStack.pop();
        // 2. Update height of this ancestor node
            node.height = 1 + max(height(node.left), height(node.right));

        // 3. Get the balance factor of this ancestor
        //      node to check whether this node became
        //      unbalanced
            int balance = getBalance(node);

            // If this node becomes unbalanced, then there
            // are 4 cases Left Left Case
            if (balance > 1 && payload.compareLatTo(node.left.payload) < 0)
                rightRotate(node);

            // Right Right Case
            if (balance < -1 && payload.compareLatTo(node.right.payload) > 0)
                leftRotate(node);

            // Left Right Case
            if (balance > 1 && payload.compareLatTo(node.left.payload) > 0) {
                node.left = leftRotate(node.left);
                rightRotate(node);
            }

            // Right Left Case
            if (balance < -1 && payload.compareLatTo(node.right.payload) < 0) {
                node.right = rightRotate(node.right);
                leftRotate(node);
            }
        }
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
