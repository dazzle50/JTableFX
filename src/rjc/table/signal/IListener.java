/**************************************************************************
 *  Copyright (C) 2025 by Richard Crook                                   *
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

package rjc.table.signal;

/*************************************************************************************************/
/******************************** Interface for signal listeners *********************************/
/*************************************************************************************************/

/**
 * Functional interface that defines a listener for signal events.
 * Implementing classes or lambda expressions can receive signals from ISignal senders.
 * The @FunctionalInterface annotation ensures this interface has exactly one abstract method,
 * enabling use with lambda expressions and method references.
 */
@FunctionalInterface // allows compiler to generate error if not single method
public interface IListener
{
  /******************************************** slot *********************************************/
  /**
   * Receives a signal from a sender with optional message parameters.
   * This method is called when a signal is emitted by an ISignal implementation.
   * 
   * @param sender the signal sender that emitted this signal
   * @param msg variable number of message objects providing signal context and data
   */
  void slot( ISignal sender, Object... msg ); // message is array of objects

}