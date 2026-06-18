/**************************************************************************
 *  Copyright (C) 2026 by Richard Crook                                   *
 *  https://github.com/dazzle50/JTableFX                                  *
 *                                                                        *
 *  This program is free software: you can redistribute it and/or modify  *
 *  it under the terms of the GNU General Public License as published by  *
 *  the Free Software Foundation, either version 3 of the License, or     *
 *  (at your option) any later version.                                   *
 *                                                                        *
 *  This program is distributed in the hope that it will be useful,       *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *  GNU General Public License for more details.                          *
 *                                                                        *
 *  You should have received a copy of the GNU General Public License     *
 *  along with this program.  If not, see http://www.gnu.org/licenses/    *
 **************************************************************************/

package rjc.table.control;

import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Popup;
import rjc.table.Utils;
import rjc.table.view.Colours;

/*************************************************************************************************/
/************ ExpandingField that has optional button, numeric value, prefix & suffix ************/
/*************************************************************************************************/

/**
 * Base field for text controls that can show a right-hand button and step through values.
 * <p>
 * The field stores an optional prefix and suffix around a raw value, supports keyboard and mouse
 * wheel stepping, and can overflow step changes into another {@link IOverflowField}. When focused,
 * scroll events are captured with temporary event filters and released again on focus loss.
 * <p>
 * When a button is visible, extra right padding is derived from the external caller/CSS/editor
 * padding so text does not overlap the button.
 * <p>
 * Subclasses usually override {@link #setValue(Object)}, {@link #getDouble()}, or
 * {@link #changeValue(double)} when the raw value is not a simple number.
 */
public class ButtonField extends ExpandingField implements IOverflowField
{
  private double          m_minValue;            // minimum number allowed
  private double          m_maxValue;            // maximum number allowed

  private double          m_page;                // page-up/page-down value increment
  private double          m_step;                // arrow-key value increment

  private String          m_prefix;              // prefix shown before value
  private String          m_suffix;              // suffix shown after value

  private ButtonType      m_buttonType;          // button type, null means no button
  private Canvas          m_button;              // canvas to show button

  private IOverflowField  m_overflowField;       // field for overflow support
  private Parent          m_scrollRoot;          // scene root with scroll filter installed
  private Parent          m_popupParentRoot;     // popup owner root with scroll filter installed
  private Insets          m_basePadding;         // external padding before button space is added
  private boolean         m_settingPadding;      // true so derived padding is not saved as base

  public static final int BUTTONS_WIDTH_MAX = 16;
  public static final int BUTTONS_PADDING   = 2;
  public static double    LAST_CHANGE_DELTA = 0; // zero if change due to user typing

  public enum ButtonType
  {
    DOWN, UP_DOWN
  }

  // handler for mouse scroll events to step increase/decrease numeric value
  public final EventHandler<ScrollEvent> SCROLL_HANDLER = event ->
  {
    event.consume();
    double step = event.getDeltaY() > 0 ? m_step : -m_step;
    changeValue( step, event.isShiftDown(), event.isControlDown(), event.isAltDown() );
  };

  /**************************************** constructor ******************************************/
  /**
   * Creates a field with no prefix or suffix, range 1 to 999, and step/page increments of 1 and 10.
   * No button is created until {@link #setButtonType(ButtonType)} is called.
   */
  public ButtonField()
  {
    // set default spin editor characteristics
    setPrefixSuffix( null, null );
    setRange( 1.0, 999.0 );
    setStepPage( 1.0, 10.0 );
    setOnKeyPressed( event -> keyPressed( event ) );

    // remember external padding as the base before deriving extra space for the button
    paddingProperty().addListener( ( property, oldPadding, newPadding ) ->
    {
      if ( !m_settingPadding )
      {
        m_basePadding = newPadding;
        drawButton();
      }
    } );

    // remove scroll filters when the field is detached from a scene
    sceneProperty().addListener( ( property, oldScene, newScene ) ->
    {
      if ( oldScene != newScene )
        removeScrollFilters();
      if ( newScene != null && isFocused() )
        installScrollFilters();
    } );

    // when focused take control of scroll events, otherwise release control
    focusedProperty().addListener( ( property, oldFocus, newFocus ) ->
    {
      if ( newFocus )
        installScrollFilters();
      else
        removeScrollFilters();
    } );
  }

  /****************************************** setValue *******************************************/
  /**
   * Sets the displayed value, wrapping it with the current prefix and suffix.
   *
   * @param value raw value to display; null is displayed as the literal text {@code "null"}
   */
  public void setValue( Object value )
  {
    // set field text after adding prefix + suffix
    String text = value == null ? "null" : value.toString();
    setText( m_prefix + text + m_suffix );
    positionCaret( m_prefix.length() + text.length() );
  }

  /****************************************** getValue *******************************************/
  /**
   * Returns the raw value text with the current prefix and suffix removed when present.
   * <p>
   * If the displayed text does not contain the expected prefix and suffix, the full text is returned
   * unchanged.
   *
   * @return raw value text
   */
  public String getValue()
  {
    // return editor text without prefix + suffix
    String text = getText();
    if ( text == null )
      return "";

    String prefix = m_prefix == null ? "" : m_prefix;
    String suffix = m_suffix == null ? "" : m_suffix;
    int start = prefix.length();
    int end = text.length() - suffix.length();

    if ( end >= start && text.startsWith( prefix ) && text.endsWith( suffix ) )
      return text.substring( start, end );

    return text;
  }

  /***************************************** getDouble *******************************************/
  /**
   * Parses the raw value text as a double.
   *
   * @return parsed value, or one less than the minimum range value if parsing fails
   */
  public double getDouble()
  {
    // attempt to get number from field text, otherwise return below min
    try
    {
      return Double.parseDouble( getValue() );
    }
    catch ( Exception exception )
    {
      return m_minValue - 1.0;
    }
  }

  /****************************************** getPrefix ******************************************/
  /**
   * Returns the current text prefix.
   *
   * @return prefix text, never null
   */
  public String getPrefix()
  {
    // return prefix
    return m_prefix;
  }

  /****************************************** getSuffix ******************************************/
  /**
   * Returns the current text suffix.
   *
   * @return suffix text, never null
   */
  public String getSuffix()
  {
    // return suffix
    return m_suffix;
  }

  /*************************************** setPrefixSuffix ***************************************/
  /**
   * Sets the text wrapped around values displayed by this field.
   * <p>
   * This base implementation does not rewrite the current displayed text.
   *
   * @param prefix prefix text, or null for no prefix
   * @param suffix suffix text, or null for no suffix
   */
  public void setPrefixSuffix( String prefix, String suffix )
  {
    // set prefix and suffix, translating null to ""
    m_prefix = prefix == null ? "" : prefix;
    m_suffix = suffix == null ? "" : suffix;
  }

  /******************************************* getMin ********************************************/
  /**
   * Returns the minimum range value used for clamping and overflow.
   *
   * @return minimum range value
   */
  public double getMin()
  {
    // return minimum allowed spin value
    return m_minValue;
  }

  /******************************************* getMax ********************************************/
  /**
   * Returns the maximum range value used for clamping and overflow.
   *
   * @return maximum range value
   */
  public double getMax()
  {
    // return maximum allowed spin value
    return m_maxValue;
  }

  /******************************************* setRange ******************************************/
  /**
   * Sets the inclusive range used by {@link #changeValue(double)}.
   * <p>
   * Existing text is not changed. Without an overflow field, stepped values are clamped into this
   * range; with an overflow field, values wrap across the range and step the overflow field.
   *
   * @param minValue minimum allowed stepped value
   * @param maxValue maximum allowed stepped value
   * @throws IllegalArgumentException if minValue is greater than maxValue
   */
  public void setRange( double minValue, double maxValue )
  {
    // check range is valid
    if ( minValue > maxValue )
      throw new IllegalArgumentException( "Min " + minValue + " > Max " + maxValue );

    // set range used for clamping or wrapping stepped values
    m_minValue = minValue;
    m_maxValue = maxValue;
  }

  /***************************************** setStepPage *****************************************/
  /**
   * Sets the keyboard and mouse-wheel step sizes.
   *
   * @param step increment used by arrow keys, mouse wheel, and button clicks
   * @param page increment used by Page Up and Page Down
   */
  public void setStepPage( double step, double page )
  {
    // set step and page increment/decrement sizes
    m_step = step;
    m_page = page;
  }

  /**************************************** setButtonType ****************************************/
  /**
   * Creates the button if needed and sets the glyph drawn inside it.
   * <p>
   * Passing null removes the button from the field without discarding it, and restores the base
   * padding.
   *
   * @param type button glyph type, or null for no button
   */
  public void setButtonType( ButtonType type )
  {
    // null means no visible button
    m_buttonType = type;
    if ( type == null )
    {
      if ( m_button != null )
      {
        getChildren().remove( m_button );
        if ( m_basePadding == null )
          m_basePadding = getPadding();

        m_settingPadding = true;
        setPadding( m_basePadding );
        m_settingPadding = false;
      }
      return;
    }

    // create button canvas if not already created
    if ( m_button == null )
    {
      m_button = new Canvas();
      m_button.setManaged( false );
      m_button.setCursor( Cursor.DEFAULT );
      m_button.setOnMousePressed( event -> buttonPressed( event ) );
      getChildren().add( m_button );

      // add listeners to (re)draw button every time editor changes size
      heightProperty().addListener( ( property, oldHeight, newHeight ) -> drawButton() );
      widthProperty().addListener( ( property, oldWidth, newWidth ) -> drawButton() );
    }

    // ensure the button is visible if it was removed by setButtonType(null)
    if ( !getChildren().contains( m_button ) )
      getChildren().add( m_button );

    drawButton();
  }

  /****************************************** getButton ******************************************/
  /**
   * Returns the button canvas.
   *
   * @return button canvas, or null if no button has been created
   */
  public Canvas getButton()
  {
    // return button canvas
    return m_button;
  }

  /************************************* installScrollFilters *************************************/
  private void installScrollFilters()
  {
    // use filters so any existing scene scroll handler is left intact
    removeScrollFilters();

    Scene scene = getScene();
    if ( scene == null )
      return;

    m_scrollRoot = scene.getRoot();
    m_scrollRoot.addEventFilter( ScrollEvent.SCROLL, SCROLL_HANDLER );

    // if this field is on a pop-up, for example a drop-down, also capture scroll on owner node
    if ( scene.getWindow() instanceof Popup popup && popup.getOwnerNode() != null )
    {
      Scene ownerScene = popup.getOwnerNode().getScene();
      if ( ownerScene != null )
      {
        m_popupParentRoot = ownerScene.getRoot();
        m_popupParentRoot.addEventFilter( ScrollEvent.SCROLL, SCROLL_HANDLER );
      }
    }
  }

  /************************************** removeScrollFilters *************************************/
  private void removeScrollFilters()
  {
    // release scroll filters installed while this field had focus
    if ( m_scrollRoot != null )
      m_scrollRoot.removeEventFilter( ScrollEvent.SCROLL, SCROLL_HANDLER );
    if ( m_popupParentRoot != null )
      m_popupParentRoot.removeEventFilter( ScrollEvent.SCROLL, SCROLL_HANDLER );

    m_scrollRoot = null;
    m_popupParentRoot = null;
  }

  /***************************************** drawButton ******************************************/
  private void drawButton()
  {
    // null button type means no button should be visible or drawn
    if ( m_button == null || m_buttonType == null || !getChildren().contains( m_button ) )
      return;

    // determine size, waiting for layout before deriving padding
    double h = getHeight() - 2 * BUTTONS_PADDING;
    double w = getWidth() / 2;
    if ( w > BUTTONS_WIDTH_MAX )
      w = BUTTONS_WIDTH_MAX;
    if ( h <= 0.0 || w <= 0.0 )
      return;

    // derive editor insets from base padding and position the button
    if ( m_basePadding == null )
      m_basePadding = getPadding();

    Insets padding = m_basePadding;
    m_settingPadding = true;
    setPadding( new Insets( padding.getTop(), padding.getRight() + w, padding.getBottom(), padding.getLeft() ) );
    m_settingPadding = false;
    m_button.setLayoutX( getWidth() - w - BUTTONS_PADDING );
    m_button.setLayoutY( BUTTONS_PADDING );

    // set size and fill background
    m_button.setHeight( h );
    m_button.setWidth( w );
    GraphicsContext gc = m_button.getGraphicsContext2D();
    gc.setFill( Colours.BUTTON_BACKGROUND );
    gc.fillRect( 0.0, 0.0, w, h );

    // draw correct button depending on button type
    int x1, y1, y2;
    gc.setStroke( Colours.BUTTON_ARROW );
    switch ( m_buttonType )
    {
      case DOWN:
        // draw down arrow
        x1 = (int) ( w * 0.3 + 0.5 );
        y1 = (int) ( h * 0.3 + 0.5 );
        y2 = (int) ( h - y1 );
        for ( int y = y1; y <= y2; y++ )
        {
          double x = x1 + ( w * 0.5 - x1 ) / ( y2 - y1 ) * ( y - y1 );
          gc.strokeLine( x, y + .5, w - x, y + .5 );
        }
        break;

      case UP_DOWN:
        // draw up+down arrows
        x1 = (int) ( w * 0.2 + 0.5 );
        y1 = (int) ( h * 0.1 + 0.6 );
        y2 = (int) ( h * 0.5 - y1 );
        for ( int y = y1; y <= y2; y++ )
        {
          double x = x1 + ( w * 0.5 - x1 ) / ( y2 - y1 ) * ( y2 - y );
          gc.strokeLine( x, y + .5, w - x, y + .5 );
          gc.strokeLine( x, h - ( y + .5 ), w - x, h - ( y + .5 ) );
        }
        break;

      default:
        throw new IllegalArgumentException( "Type=" + m_buttonType );
    }
  }

  /**************************************** buttonPressed ****************************************/
  private void buttonPressed( MouseEvent event )
  {
    // if user clicked top half of buttons, step up, else step down
    requestFocus();
    event.consume();

    if ( event.getY() < m_button.getHeight() / 2 )
      changeValue( m_step, event.isShiftDown(), event.isControlDown(), event.isAltDown() );
    else
      changeValue( -m_step, event.isShiftDown(), event.isControlDown(), event.isAltDown() );
  }

  /***************************************** keyPressed ******************************************/
  /**
   * Handles default keyboard stepping for arrow, page, home, and end keys.
   *
   * @param event key event to handle
   */
  protected void keyPressed( KeyEvent event )
  {
    // action key press to change value up or down
    LAST_CHANGE_DELTA = 0.0;
    switch ( event.getCode() )
    {
      case DOWN:
        changeValue( -m_step );
        event.consume();
        break;
      case PAGE_DOWN:
        changeValue( -m_page );
        event.consume();
        break;
      case UP:
        changeValue( m_step );
        event.consume();
        break;
      case PAGE_UP:
        changeValue( m_page );
        event.consume();
        break;
      case HOME:
        setValue( m_minValue );
        event.consume();
        break;
      case END:
        setValue( m_maxValue );
        event.consume();
        break;
      default:
        break;
    }
  }

  /***************************************** changeValue *****************************************/
  /**
   * Changes the numeric value by the supplied delta.
   * <p>
   * If an overflow field is set, values beyond the range wrap and step the overflow field.
   * Otherwise values are clamped into range.
   *
   * @param delta amount to add to the current numeric value
   */
  @Override
  public void changeValue( double delta )
  {
    // change spin value by delta overflowing to wrap-field if available
    double num = getDouble() + delta;
    LAST_CHANGE_DELTA = delta;
    // if no overflow field, clamp the value to between min & max
    if ( m_overflowField == null )
      num = Utils.clamp( num, m_minValue, m_maxValue );
    else
    {
      // if overflow field step its value as necessary
      double range = 1 + m_maxValue - m_minValue;
      while ( num < m_minValue )
      {
        m_overflowField.changeValue( -1 );
        num += range;
      }
      while ( num > m_maxValue )
      {
        m_overflowField.changeValue( 1 );
        num -= range;
      }
    }

    setValue( num );
  }

  /************************************** setOverflowField ***************************************/
  /**
   * Sets the field to step when this field wraps beyond its range.
   *
   * @param overflow overflow field, or null to disable overflow and clamp values
   */
  public void setOverflowField( IOverflowField overflow )
  {
    // set overflow field to step when this spin goes beyond min or max
    m_overflowField = overflow;
  }

}