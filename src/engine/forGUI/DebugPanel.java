package engine.forGUI;

import javax.swing.*;
import java.awt.*;
import java.util.Observable;
import java.util.Observer;

/**
 * The DebugPanel class represents a specialized panel for displaying debug information and messages.
 * It extends JPanel and implements Observer to receive and display updates from observed components.
 * <p>
 * This panel provides a text area for displaying real-time information about the current state of the
 * chess application, such as AI calculations, move evaluations, and other debugging details. The panel
 * automatically refreshes when updates occur in observed objects.
 * <p>
 * The panel is designed to be placed at the bottom of the main interface and maintains a fixed size
 * to ensure consistent layout while providing adequate space for displaying messages.
 *
 * @author dareTo81
 * @author Aaron Ho
 */
class DebugPanel extends JPanel implements Observer {

  /** The preferred dimensions of the debug panel for consistent UI layout. */
  private static final Dimension CHAT_PANEL_DIMENSION = new Dimension(550, 50);

  /** The text area component used to display debug messages. */
  private final JTextArea jTextArea;

  /**
   * Constructs a new DebugPanel with a border layout and initializes the text area.
   * The panel is configured with a specific preferred size and made visible.
   */
  public DebugPanel() {
    super(new BorderLayout());
    this.jTextArea = new JTextArea();
    add(this.jTextArea);
    setPreferredSize(CHAT_PANEL_DIMENSION);
    validate();
    setVisible(true);
  }

  /**
   * Validates and refreshes the debug panel to update its visual appearance.
   * Called to ensure changes to the panel's components are properly displayed.
   */
  public void redo() {
    validate();
  }

  /**
   * Sets the text content of the debug panel to the specified message.
   * Uses SwingUtilities.invokeLater to ensure thread-safe text updates.
   *
   * @param text The message text to be displayed in the debug panel.
   */
  public void addText(final String text) {
    SwingUtilities.invokeLater(() -> this.jTextArea.setText(text));
  }

  /**
   * Updates the content of the debug panel based on changes in the observed object.
   * Automatically called when an Observable object notifies its observers of a change.
   *
   * @param obs The Observable object that triggered the update notification.
   * @param obj The object containing the information to be displayed in the debug panel.
   */
  @Override
  public void update(final Observable obs,
                     final Object obj) {
    SwingUtilities.invokeLater(() -> {
      this.jTextArea.setText(obj.toString().trim());
      redo();
    });
  }
}