package engine.forGUI;

import javax.swing.*;
import java.awt.*;
import java.util.Observable;
import java.util.Observer;

/**
 * Represents a panel for displaying debug information or messages. This panel can be used to show real-time updates or
 * information about the current state of the application. This panel is designed to be observed by objects that provide updates.
 * When updates occur, the panel's content is updated with the new information.
 * <br><br>
 * (Observer is deprecated, but its use here was advised)
 *
 * @author dareTo81
 * @author Aaron Ho
 */
class DebugPanel extends JPanel implements Observer {

  /*** The preferred dimensions of the chat panel. */
  private static final Dimension CHAT_PANEL_DIMENSION = new Dimension(550, 50);
  
  /*** The JTextArea used to display debug messages. */
  private final JTextArea jTextArea;

  /**
   * Constructs a new DebugPanel by setting up the layout and initializing the text area for displaying debug messages.
   * The panel provides a space for displaying real-time updates or application-related information.
   */
  public DebugPanel() {
    super(new BorderLayout());
    this.jTextArea = new JTextArea();
    add(this.jTextArea);
    setPreferredSize(CHAT_PANEL_DIMENSION);
    validate();
    setVisible(true);
  }

  /*** Redraws or updates the debug panel. This method can be called to trigger a visual update or to refresh the panel's contents. */
  public void redo() {
    validate();
  }

  public void addText(final String text) {
    SwingUtilities.invokeLater(() -> this.jTextArea.setText(text));
  }

  /**
   * Updates the content of the debug panel based on changes in the observed object. The method receives an object
   * (usually a message or information) and updates the text area with its contents.
   *
   * @param obs The observed object (usually not used directly in this context).
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
