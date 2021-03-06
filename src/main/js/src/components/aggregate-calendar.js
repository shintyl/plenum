import React from "react";

import Cell from './cell'
import {getTime, populateCalendar} from "../services/calendar-manager";
import CalendarTemplate from "./calendar-template";

const aggregate_calendar = (props) => {
  
    let agg = props.agg

    let calendar = new Array(24 * 4)

    calendar = populateCalendar(calendar);

    //set up calendar data
    for (let i = 0; i < 24 * 4; i++) {
        for (let j = 0; j < 7; j++) {
            calendar[i][j] = {
                num_avail: 0,
                users_avail: []
            }
        }
    }
    let users_in_event = Object.keys(agg)
    let num_users_in_event = users_in_event.length

    //combine two calendar data
    for (const i in agg) {
        agg[i].dates.forEach(element => {
            element.times.forEach(function(time) {
                calendar[time][element.date].num_avail = calendar[time][element.date].num_avail + 1
                calendar[time][element.date].users_avail.push(agg[i].userName)
            })
        })
    }



    //set up display layout
    function linspace(startValue, stopValue, cardinality) {
        let arr = [];
        let step = (stopValue - startValue) / (cardinality - 1);
        for (let i = 0; i < cardinality; i++) {
            arr.push(startValue + (step * i));
        }
        return arr;
    }
    // for n users you need n + 1 opacities
    function get_opacity_from_num_avail(num_avail) {
        if (num_users_in_event <= 6) {
            let opacities = linspace(0, 1, num_users_in_event + 1)
            return opacities[num_avail]
        } else {
            let opacities = linspace(0, 1, 8)
            if (num_avail === 0) {
                return 0
            } else if (num_avail === num_users_in_event) {
                return 1
            } else {
                for (let i = 0; i < 6; i++) {
                    if ((num_avail / num_users_in_event) > (i / 6) && (num_avail / num_users_in_event) < ((i + 1) / 6)) {
                        return opacities[i + 1]
                    }
                }
            }
        }
    }

    calendar = calendar.slice(props.timeRange[0]*4, props.timeRange[1]*4)
    console.log(props.timeRange)

    return (
        <CalendarTemplate startTime={props.timeRange[0]}>
            {calendar.map((rows, i) => (
                <React.Fragment key={i}>
                    {rows.map((cell, j) => {
                        return <Cell
                            key={j}
                            tooltip_id={'' + i + j}
                            opacity={get_opacity_from_num_avail(calendar[i][j].num_avail)}
                            users_avail={calendar[i][j].users_avail}
                        />

                    })}
                </React.Fragment>
            ))}
        </CalendarTemplate>
    )
};

export default aggregate_calendar