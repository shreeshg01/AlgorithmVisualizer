import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.Arrays;
import java.util.Random;

/**
 * Single-file Java Swing sorting algorithm visualizer with:
 * - Bubble Sort, Quick Sort, Merge Sort
 * - Randomize, Start, Stop, Reset
 * - Algorithm dropdown + speed slider
 * - Animated bars with color coding
 *
 * Color Coding:
 * White: default
 * Red: comparing/swapping
 * Green: confirmed sorted (Bubble shows progressive confirmation; others turn green at the end)
 */
public class AlgorithmVisualizer extends JFrame {

    // -------------------- UI / State --------------------
    private static final int DEFAULT_ARRAY_SIZE = 80;
    private static final int MIN_VALUE = 5;
    private static final int MAX_VALUE = 300;

    private final VisualPanel visualPanel = new VisualPanel();

    private final JButton randomizeBtn = new JButton("Randomize");
    private final JButton startBtn = new JButton("Start");
    private final JButton stopBtn = new JButton("Stop");
    private final JButton resetBtn = new JButton("Reset");

    private final JComboBox<String> algoCombo = new JComboBox<>(new String[]{"Bubble Sort", "Quick Sort", "Merge Sort"});
    private final JSlider speedSlider = new JSlider(1, 200, 30); // delay ms

    private volatile boolean sorting = false;
    private SortWorker currentWorker = null;

    // Data being visualized
    private int[] array = new int[DEFAULT_ARRAY_SIZE];

    // Snapshot of array taken right when Start is pressed (used by Reset)
    private int[] startSnapshot = null;

    // Highlight indices for RED comparing/swapping
    private volatile int hiA = -1;
    private volatile int hiB = -1;

    // Mark sorted indices for GREEN
    private volatile boolean[] sorted = new boolean[DEFAULT_ARRAY_SIZE];

    public AlgorithmVisualizer() {
        super("Sorting Algorithm Visualizer (Swing)");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));

        controls.add(new JLabel("Algorithm:"));
        controls.add(algoCombo);

        controls.add(randomizeBtn);
        controls.add(startBtn);
        controls.add(stopBtn);
        controls.add(resetBtn);

        controls.add(new JLabel("Speed:"));
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        speedSlider.setMajorTickSpacing(50);
        speedSlider.setMinorTickSpacing(10);
        controls.add(speedSlider);

        add(controls, BorderLayout.NORTH);
        add(visualPanel, BorderLayout.CENTER);

        // Initial button states
        stopBtn.setEnabled(false);
        resetBtn.setEnabled(true);

        // Actions
        randomizeBtn.addActionListener(e -> {
            if (sorting) return;
            randomizeArray();
        });

        startBtn.addActionListener(e -> {
            if (sorting) return;
            startSorting();
        });

        // Stop: cancel current worker and leave array as-is (mid-sort state)
        stopBtn.addActionListener(e -> stopSorting());

        // Reset: cancel if running, then restore snapshot from Start
        resetBtn.addActionListener(e -> resetToSnapshot());

        speedSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                // delay is read dynamically; nothing else needed
            }
        });

        setSize(1100, 650);
        setLocationRelativeTo(null);

        randomizeArray();
    }

    private void randomizeArray() {
        Random r = new Random();
        for (int i = 0; i < array.length; i++) {
            array[i] = r.nextInt(MAX_VALUE - MIN_VALUE + 1) + MIN_VALUE;
        }
        Arrays.fill(sorted, false);
        hiA = hiB = -1;
        startSnapshot = null; // snapshot becomes invalid after randomize
        visualPanel.repaint();
    }

    private void startSorting() {
        sorting = true;

        // Take snapshot for Reset (pre-sort state)
        startSnapshot = Arrays.copyOf(array, array.length);

        // Disable controls that could corrupt state during sorting
        startBtn.setEnabled(false);
        randomizeBtn.setEnabled(false);
        algoCombo.setEnabled(false);

        // Enable stop; reset can still be used (it will cancel + restore snapshot)
        stopBtn.setEnabled(true);
        resetBtn.setEnabled(true);

        Arrays.fill(sorted, false);
        hiA = hiB = -1;
        visualPanel.repaint();

        String selected = (String) algoCombo.getSelectedItem();
        currentWorker = new SortWorker(selected);
        currentWorker.execute();
    }

    private void stopSorting() {
        if (!sorting) return;

        // Cancel worker; sorting loop checks isCancelled()
        if (currentWorker != null) {
            currentWorker.cancel(true);
        }

        // UI will be re-enabled in done(), but we can also be proactive:
        // We'll leave the array mid-sort; user can choose Reset or Randomize.
    }

    private void resetToSnapshot() {
        // If sorting is running, cancel it first (safe cancellation)
        if (sorting && currentWorker != null) {
            currentWorker.cancel(true);
        }

        // Restore snapshot ONLY if we have one
        if (startSnapshot != null) {
            array = Arrays.copyOf(startSnapshot, startSnapshot.length);
            // Important: sorted array length depends on array length
            sorted = new boolean[array.length];
        } else {
            // No snapshot? Then reset basically means "clear highlights/greens".
            Arrays.fill(sorted, false);
        }

        hiA = hiB = -1;
        Arrays.fill(sorted, false);
        visualPanel.repaint();

        // If cancellation is in-flight, done() will re-enable controls when it finishes.
        // If not sorting, ensure controls are usable.
        if (!sorting) {
            startBtn.setEnabled(true);
            randomizeBtn.setEnabled(true);
            algoCombo.setEnabled(true);
            stopBtn.setEnabled(false);
        }
    }

    // Slider value to delay ms. Lower = faster.
    private int getDelayMs() {
        return Math.max(1, speedSlider.getValue());
    }

    /**
     * Sleep helper used during sorting steps.
     * If the worker is cancelled, we want to wake up quickly.
     */
    private void pause(SortWorker w) {
        try {
            Thread.sleep(getDelayMs());
        } catch (InterruptedException e) {
            // If interrupted due to cancel(true), we exit quickly.
            if (w != null && w.isCancelled()) {
                // do nothing, caller will stop naturally
            }
            Thread.currentThread().interrupt(); // preserve interrupt flag
        }
    }

    private void highlight(int a, int b) {
        hiA = a;
        hiB = b;
        visualPanel.repaint();
    }

    private void clearHighlight() {
        hiA = -1;
        hiB = -1;
        visualPanel.repaint();
    }

    /**
     * Swap two elements with repaint + delay (core "animation step").
     */
    private void swap(int i, int j, SortWorker w) {
        int tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;

        visualPanel.repaint();
        pause(w);
    }

    private void markSorted(int idx) {
        if (idx >= 0 && idx < sorted.length) {
            sorted[idx] = true;
            visualPanel.repaint();
        }
    }

    private void markAllSorted() {
        Arrays.fill(sorted, true);
        clearHighlight();
        visualPanel.repaint();
    }

    // -------------------- SwingWorker Sorting --------------------
    private class SortWorker extends SwingWorker<Void, Void> {
        private final String algorithm;

        SortWorker(String algorithm) {
            this.algorithm = algorithm;
        }

        @Override
        protected Void doInBackground() {
            if ("Bubble Sort".equals(algorithm)) {
                bubbleSort();
            } else if ("Quick Sort".equals(algorithm)) {
                quickSort(0, array.length - 1);
            } else if ("Merge Sort".equals(algorithm)) {
                mergeSort(0, array.length - 1, new int[array.length]);
            }
            return null;
        }

        @Override
        protected void done() {
            // Re-enable controls safely on EDT
            sorting = false;

            startBtn.setEnabled(true);
            randomizeBtn.setEnabled(true);
            algoCombo.setEnabled(true);

            stopBtn.setEnabled(false);
            resetBtn.setEnabled(true);

            // If cancelled, DO NOT force everything green.
            // Cancellation means user wanted to stop, not claim "sorted".
            if (!isCancelled()) {
                markAllSorted();
            } else {
                clearHighlight();
            }
        }

        // -------------------- Bubble Sort --------------------
        private void bubbleSort() {
            int n = array.length;
            for (int end = n - 1; end > 0; end--) {
                if (isCancelled()) return;

                boolean swapped = false;
                for (int i = 0; i < end; i++) {
                    if (isCancelled()) return;

                    highlight(i, i + 1);
                    pause(this);

                    if (isCancelled()) return;

                    if (array[i] > array[i + 1]) {
                        swap(i, i + 1, this);
                        swapped = true;
                    }
                }

                markSorted(end);

                if (!swapped) {
                    for (int k = end - 1; k >= 0; k--) {
                        if (isCancelled()) return;
                        markSorted(k);
                    }
                    break;
                }
            }

            if (!isCancelled()) markSorted(0);
            clearHighlight();
        }

        // -------------------- Quick Sort --------------------
        private void quickSort(int low, int high) {
            if (isCancelled()) return;
            if (low >= high) return;

            int pivotIndex = partition(low, high);
            if (isCancelled()) return;

            quickSort(low, pivotIndex - 1);
            quickSort(pivotIndex + 1, high);
        }

        private int partition(int low, int high) {
            int pivot = array[high];
            int i = low - 1;

            for (int j = low; j < high; j++) {
                if (isCancelled()) return low; // early exit (value doesn't matter much if cancelled)

                highlight(j, high);
                pause(this);

                if (isCancelled()) return low;

                if (array[j] <= pivot) {
                    i++;
                    if (i != j) {
                        highlight(i, j);
                        pause(this);
                        if (isCancelled()) return low;
                        swap(i, j, this);
                    }
                }
            }

            if (isCancelled()) return low;

            if (i + 1 != high) {
                highlight(i + 1, high);
                pause(this);
                if (isCancelled()) return low;
                swap(i + 1, high, this);
            }

            clearHighlight();
            return i + 1;
        }

        // -------------------- Merge Sort --------------------
        private void mergeSort(int left, int right, int[] temp) {
            if (isCancelled()) return;
            if (left >= right) return;

            int mid = left + (right - left) / 2;

            mergeSort(left, mid, temp);
            mergeSort(mid + 1, right, temp);
            merge(left, mid, right, temp);
        }

        private void merge(int left, int mid, int right, int[] temp) {
            if (isCancelled()) return;

            for (int i = left; i <= right; i++) {
                if (isCancelled()) return;
                temp[i] = array[i];
            }

            int i = left;
            int j = mid + 1;
            int k = left;

            while (i <= mid && j <= right) {
                if (isCancelled()) return;

                highlight(i, j);
                pause(this);

                if (isCancelled()) return;

                if (temp[i] <= temp[j]) {
                    array[k] = temp[i];
                    i++;
                } else {
                    array[k] = temp[j];
                    j++;
                }

                visualPanel.repaint();
                pause(this);
                k++;
            }

            while (i <= mid) {
                if (isCancelled()) return;

                highlight(i, -1);
                pause(this);

                if (isCancelled()) return;

                array[k] = temp[i];
                i++;
                k++;

                visualPanel.repaint();
                pause(this);
            }

            while (j <= right) {
                if (isCancelled()) return;

                highlight(j, -1);
                pause(this);

                if (isCancelled()) return;

                array[k] = temp[j];
                j++;
                k++;

                visualPanel.repaint();
                pause(this);
            }

            clearHighlight();
        }
    }

    // -------------------- Visualization Panel --------------------
    private class VisualPanel extends JPanel {
        VisualPanel() {
            setBackground(Color.DARK_GRAY);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                if (array == null || array.length == 0) return;

                int n = array.length;

                int max = 1;
                for (int v : array) max = Math.max(max, v);

                int barW = Math.max(1, w / n);
                int gap = 1;

                for (int i = 0; i < n; i++) {
                    int val = array[i];
                    int barH = (int) ((val / (double) max) * (h - 30));
                    int x = i * barW;
                    int y = h - barH;

                    Color c = Color.WHITE;

                    if (sorted != null && i < sorted.length && sorted[i]) {
                        c = Color.GREEN;
                    }
                    if (i == hiA || i == hiB) {
                        c = Color.RED;
                    }

                    g2.setColor(c);
                    int drawW = Math.max(1, barW - gap);
                    g2.fillRect(x, y, drawW, barH);
                }

                g2.setColor(Color.LIGHT_GRAY);
                g2.drawString("White=default, Red=comparing/swapping, Green=sorted", 12, 18);

            } finally {
                g2.dispose();
            }
        }
    }

    // -------------------- Main --------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AlgorithmVisualizer app = new AlgorithmVisualizer();
            app.setVisible(true);
        });
    }
}
