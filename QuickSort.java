/*************************************************************************
 *  Compilation:  javac Quicksort.java
 *  Execution:    java Quicksort N
 *  
 *  Generate N random real numbers between 0 and 1 and quicksort them.
 *
 *************************************************************************/

public class QuickSort
{
    private static long comparisons = 0;
    private static long exchanges   = 0;


   /***********************************************************************
    *  Quicksort code from Sedgewick 7.1, 7.2.
    ***********************************************************************/
    public static void quicksort(int[] a, int length)
    {
        quicksort(a, 0, length - 1);
//        quicksort(a, 0, a.length - 1);
    }

    public static void quicksort(int[] a, int left, int right)
    { 
        if (right<=left)
            return;
        int i = partition(a, left, right);
        quicksort(a, left, i-1);
        quicksort(a, i+1, right);
    }

    public static int partition(int[] a, int left, int right)
    {
        int i = left - 1;
        int j = right;

        while(true)
        {
            // find item on left to swap
            while (less(a[++i], a[right]));
            // find item on right to swap
            while (less(a[right], a[--j]))
                if (j == left)
                    break;
            // check if pointers cross
            if (i >= j)
                break;
            exch(a, i, j);
        }
        // swap with partition element
        exch(a, i, right);
        return i;
    }

    // is x < y ?
    public static boolean less(int x, int y)
    {
        comparisons++;
        return (x<y);
    }

    // exchange a[i] and a[j]
    static void exch(int[] a, int i, int j)
    {
        exchanges++;
        int swap = a[i];
        a[i] = a[j];
        a[j] = swap;
    }
}