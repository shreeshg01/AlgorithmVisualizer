import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.Arrays;
import java.util.Random;

/**
 * Single-file Java Swing sorting algorithm visualizer.
 *
 * Requirements covered:
 * - Bubble Sort, Quick Sort, Merge Sort
 * - Randomize + Start buttons
 * - Algorithm dropdown (JComboBox)
 * - Speed slider (delay)
 * - Bars drawn in JPanel (histogram)
 * - Step-by-step animation via SwingWorker (UI stays responsive)
 * - Color coding: White default, Red comparing/swapping, Green confirmed sorted
 * - Start disabled while running
 */
public class AlgorithmVisualizer extends JFrame {

    // -------------------- UI / State --------------------
    private static final int DEFAULT_ARRAY_SIZE = 80;
    private static final int MIN_VALUE = 5;
    private static final int MAX_VALUE = 300;

    private final VisualPanel visualPanel = new VisualPanel();

    private final JButton randomizeBtn = new JButton("Randomize");
    private final JButton startBtn = new JButton("Start");
    private final JComboBox<String> algoCombo = new JComboBox<>(new String[]{"Bubble Sort", "Quick Sort", "Merge Sort"});
    private final JSlider speedSlider = new JSlider(1, 200, 30); // value maps to delay ms

    private volatile boolean sorting = false;
    private SortWorker currentWorker = null;

    // Data being visualized
    private int[] array = new int[DEFAULT_ARRAY_SIZE];

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

        controls.add(new JLabel("Speed:"));
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        speedSlider.setMajorTickSpacing(50);
        speedSlider.setMinorTickSpacing(10);
        controls.add(speedSlider);

        add(controls, BorderLayout.NORTH);

        // Center visualization
        add(visualPanel, BorderLayout.CENTER);

        // Wire actions
        randomizeBtn.addActionListener(e -> {
            if (sorting) return;
            randomizeArray();
        });

        startBtn.addActionListener(e -> {
            if (sorting) return;
            startSorting();
        });

        // Optional: make slider feel immediate (no extra logic needed; delay read dynamically)
        speedSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                // Nothing required; delay is read on the fly.
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
        visualPanel.repaint();
    }

    private void startSorting() {
        sorting = true;
        startBtn.setEnabled(false);
        randomizeBtn.setEnabled(false);
        algoCombo.setEnabled(false);

        Arrays.fill(sorted, false);
        hiA = hiB = -1;
        visualPanel.repaint();

        String selected = (String) algoCombo.getSelectedItem();
        currentWorker = new SortWorker(selected);
        currentWorker.execute();
    }

    // Convert slider value to delay in ms.
    // Lower slider => faster (small delay), higher => slower (bigger delay)
    private int getDelayMs() {
        // 1..200 -> 1..200 ms
        return Math.max(1, speedSlider.getValue());
    }

    // Sleep helper used inside sorting steps.
    private void pause() {
        try {
            Thread.sleep(getDelayMs());
        } catch (InterruptedException ignored) {
        }
    }

    // Update highlight indices (RED) and repaint immediately
    private void highlight(int a, int b) {
        hiA = a;
        hiB = b;
        visualPanel.repaint();
    }

    // Clear highlight indices
    private void clearHighlight() {
        hiA = -1;
        hiB = -1;
        visualPanel.repaint();
    }

    // Swap two elements, with repaint + delay (this is the core "animated step")
    private void swap(int i, int j) {
        int tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;

        // After the data change, repaint to show new bar positions/heights.
        visualPanel.repaint();

        // Delay controls how fast the viewer sees changes.
        pause();
    }

    // Mark an index as sorted (GREEN) and repaint
    private void markSorted(int idx) {
        if (idx >= 0 && idx < sorted.length) {
            sorted[idx] = true;
            visualPanel.repaint();
        }
    }

    // Mark all indices as sorted (GREEN) at the end
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
            // Run sorting in background thread so EDT (UI thread) stays responsive.
            if ("Bubble Sort".equals(algorithm)) {
                bubbleSort();
            } else if ("Quick Sort".equals(algorithm)) {
                quickSort(0, array.length - 1);
                // Quick sort doesn't naturally "confirm" indices one-by-one.
                // We'll mark all at end for the required GREEN state.
            } else if ("Merge Sort".equals(algorithm)) {
                mergeSort(0, array.length - 1, new int[array.length]);
                // Similar: mark all at end.
            }
            return null;
        }

        @Override
        protected void done() {
            // Back on EDT: re-enable controls safely.
            sorting = false;
            startBtn.setEnabled(true);
            randomizeBtn.setEnabled(true);
            algoCombo.setEnabled(true);

            // Ensure everything ends green.
            markAllSorted();
        }

        // -------------------- Bubble Sort (step-by-step compare+swap) --------------------
        private void bubbleSort() {
            int n = array.length;
            for (int end = n - 1; end > 0; end--) {
                boolean swapped = false;
                for (int i = 0; i < end; i++) {
                    // Highlight the bars being compared (RED)
                    highlight(i, i + 1);
                    pause();

                    // If out of order, swap (swap triggers repaint+pause)
                    if (array[i] > array[i + 1]) {
                        swap(i, i + 1);
                        swapped = true;
                    }
                }
                // After each pass, the element at 'end' is in correct final position
                markSorted(end);

                if (!swapped) {
                    // Array already sorted: mark remaining as sorted and break
                    for (int k = end - 1; k >= 0; k--) markSorted(k);
                    break;
                }
            }
            markSorted(0);
            clearHighlight();
        }

        // -------------------- Quick Sort (partition + recursion) --------------------
        private void quickSort(int low, int high) {
            if (low >= high) return;

            int pivotIndex = partition(low, high);

            // Pivot is in its final position after partition
            // We can mark it sorted *if* we want, but Quick Sort's "confirmed sorted"
            // is only strictly true when recursion completes. We'll keep marking at the end.
            quickSort(low, pivotIndex - 1);
            quickSort(pivotIndex + 1, high);
        }

        private int partition(int low, int high) {
            int pivot = array[high];
            int i = low - 1;

            for (int j = low; j < high; j++) {
                // Compare current element with pivot (highlight RED)
                highlight(j, high);
                pause();

                if (array[j] <= pivot) {
                    i++;
                    if (i != j) {
                        // Swap places (animated)
                        highlight(i, j);
                        pause();
                        swap(i, j);
                    }
                }
            }

            // Put pivot in correct position
            if (i + 1 != high) {
                highlight(i + 1, high);
                pause();
                swap(i + 1, high);
            }
            clearHighlight();
            return i + 1;
        }

        // -------------------- Merge Sort (merge steps animated) --------------------
        private void mergeSort(int left, int right, int[] temp) {
            if (left >= right) return;
            int mid = left + (right - left) / 2;

            mergeSort(left, mid, temp);
            mergeSort(mid + 1, right, temp);
            merge(left, mid, right, temp);
        }

        private void merge(int left, int mid, int right, int[] temp) {
            // Copy current segment into temp
            for (int i = left; i <= right; i++) temp[i] = array[i];

            int i = left;      // pointer in left half
            int j = mid + 1;   // pointer in right half
            int k = left;      // pointer for merged output back into array

            while (i <= mid && j <= right) {
                // Highlight compared elements (RED)
                highlight(i, j);
                pause();

                if (temp[i] <= temp[j]) {
                    // Write value back to array (this is an "animated step")
                    array[k] = temp[i];
                    i++;
                } else {
                    array[k] = temp[j];
                    j++;
                }

                // Repaint after each write so the viewer sees the build-up.
                visualPanel.repaint();
                pause();
                k++;
            }

            // Copy remaining left half (if any)
            while (i <= mid) {
                highlight(i, -1);
                pause();

                array[k] = temp[i];
                i++;
                k++;

                visualPanel.repaint();
                pause();
            }

            // Remaining right half is already in place in many merge implementations,
            // but since we copied from temp -> array, we should write it back too.
            while (j <= right) {
                highlight(j, -1);
                pause();

                array[k] = temp[j];
                j++;
                k++;

                visualPanel.repaint();
                pause();
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

                // Determine max for scaling bar heights
                int max = 1;
                for (int v : array) max = Math.max(max, v);

                // Bar width (at least 1 pixel)
                int barW = Math.max(1, w / n);
                int gap = 1; // small gap for readability

                for (int i = 0; i < n; i++) {
                    int val = array[i];

                    // Scale height to panel
                    int barH = (int) ((val / (double) max) * (h - 30)); // leave top padding
                    int x = i * barW;
                    int y = h - barH;

                    // Color rules:
                    // - GREEN if sorted[i] true
                    // - RED if i is highlighted (hiA or hiB)
                    // - WHITE otherwise
                    Color c = Color.WHITE;

                    if (sorted != null && i < sorted.length && sorted[i]) {
                        c = Color.GREEN;
                    }
                    if (i == hiA || i == hiB) {
                        c = Color.RED;
                    }

                    g2.setColor(c);

                    // Draw bar (slightly narrower to show gaps)
                    int drawW = Math.max(1, barW - gap);
                    g2.fillRect(x, y, drawW, barH);
                }

                // Small footer text
                g2.setColor(Color.LIGHT_GRAY);
                g2.drawString("White=default, Red=comparing/swapping, Green=sorted", 12, 18);

            } finally {
                g2.dispose();
            }
        }
    }

    // -------------------- Main --------------------
    public static void main(String[] args) {
        // Always create and show Swing UI on the EDT.
        SwingUtilities.invokeLater(() -> {
            AlgorithmVisualizer app = new AlgorithmVisualizer();
            app.setVisible(true);
        });
    }
}
