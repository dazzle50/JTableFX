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

import rjc.table.signal.ObservableStatus;

/*************************************************************************************************/
/*********************** Interface for objects that have observable-status ***********************/
/*************************************************************************************************/

/**
 * Defines a contract for objects that maintain an observable status.
 * 
 * <p> Implementing classes must provide a mechanism to set and retrieve an
 * {@link ObservableStatus} object, which typically represents the current
 * validation, error, warning, or informational state of a control or field.
 */
public interface IObservableStatus
{
  /***************************************** setStatus *******************************************/
  /**
   * Sets the current status of the implementing object.
   * <p>
   * The provided {@link ObservableStatus} object carries information about
   * the state (e.g., valid, warning, error) and an associated message.
   * Observers registered with the status object will be notified of changes.
   *
   * @param status the new status to be applied; may be {@code null} if supported
   *               by the implementation
   */
  public void setStatus( ObservableStatus status ); // set text field status

  /***************************************** getStatus *******************************************/
  /**
   * Returns the current status of the implementing object.
   *
   * @return the current {@link ObservableStatus}, or {@code null} if no status
   *         has been set or the implementation supports a null state
   */
  public ObservableStatus getStatus(); // return text field status

}
