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
 * chess application, such as AI calculations, move evaluations, and other debugging details. When
 * updates occur in observed objects, the panel automatically refreshes to display the latest information.
 * <p>
 * The panel is designed to be placed at the bottom of the main interface and maintains a fixed size
 * to ensure consistent layout while still providing adequate space for displaying messages.
 * <p>
 * Note: This class uses the Observer interface which is deprecated in newer Java versions but is
 * still appropriately implemented here as advised for the application architecture.
 *
 * @author dareTo81
 * @author Aaron Ho
 */
class DebugPanel extends JPanel implements Observer {

  /**
   * The preferred dimensions of the debug panel. This constant defines the width and height
   * of the panel to ensure consistent sizing in the user interface layout.
   */
  private static final Dimension CHAT_PANEL_DIMENSION = new Dimension(550, 50);

  /**
   * The text area component used to display debug messages. This field stores a reference
   * to the JTextArea where all debug information is rendered for user viewing.
   */
  private final JTextArea jTextArea;

  /**
   * Constructs a new DebugPanel with a border layout and initializes the text area for displaying messages.
   * The panel is configured with a specific preferred size to maintain consistent UI layout,
   * and the text area is added to the panel to display the debug information.
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
   * Validates and refreshes the debug panel, triggering a visual update of its contents.
   * This method is called to ensure that any changes to the panel's components or
   * layout are properly displayed after modifications have been made.
   */
  public void redo() {
    validate();
  }

  /**
   * Sets the text content of the debug panel to the specified message.
   * This method uses SwingUtilities.invokeLater to ensure that text updates
   * occur on the Event Dispatch Thread, preventing potential threading issues.
   *
   * @param text The message text to be displayed in the debug panel.
   */
  public void addText(final String text) {
    SwingUtilities.invokeLater(() -> this.jTextArea.setText(text));
  }

  /**
   * Updates the content of the debug panel based on changes in the observed object.
   * This method is called automatically when an Observable object that this panel
   * is observing notifies its observers of a change. The update will display the
   * received object's string representation in the text area and refresh the panel.
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