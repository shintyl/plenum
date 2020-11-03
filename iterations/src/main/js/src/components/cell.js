import React, { useState } from "react";
import ReactTooltip from "react-tooltip";


const cell = (props) => {
    let styles = { border: '1px solid black', width: '25px', height: '25px', textAlign: 'center', outline: 'none' }

    if (props.unavailable === 'A') {
        styles.backgroundColor = 'red'
    }

    if (props.opacity !== null) {
        styles.backgroundColor = `hsla(120, 50%, 50%, ${props.opacity}`
    }

    let tooltip = (!props.users_avail.length || props.users_avail === null)
        ? null 
        : <ReactTooltip id={props.tooltip_id} place="top" effect="solid">
            <ul style={{margin: '0', padding: '0'}}>
                {props.users_avail.map(element => <li>{element}</li>)}
            </ul>
        </ReactTooltip>

    return (
        <td>
            <button style={styles} className={"day" + props.day} value={props.time} data-tip data-for={props.tooltip_id} />
            {tooltip}
        </td>
    )

};

export default cell