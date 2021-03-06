import React, {useState, useEffect} from 'react'
import ReactDOM from 'react-dom'
import ViewableListItem from "../components/viewable-list-item";
import Header from "../components/header";
import {Typography} from "@material-ui/core";
import AddIcon from '@material-ui/icons/Add';
import Fab from "@material-ui/core/Fab";
import List from "@material-ui/core/List";
import {makeStyles} from "@material-ui/core/styles";
import {getEvents} from "../services/event-manager";

const useStyles = makeStyles(() => ({
    root: {
        '& > *': {
            margin: 30,
        },
    },
    center: {
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        width: '90%',
    },
}))

function fetchAPI(id) {
    fetch("/api/delevent?id=" + id, {
            method: 'POST',
            mode: 'cors'
        }
    ).then(data => {
        location.reload();
    })
}

const App = () =>  {
    const classes = useStyles();
    const [events, setEvents] = useState([])

    const [idToDelete, setIdToDelete] = useState(-1)

    getEvents(setEvents, '?all=true')

    useEffect(() => {
        if(idToDelete > 0) {
            fetchAPI(idToDelete)
            setIdToDelete(-1)
        }
    }, [idToDelete])

    const handleAdd = () => {
        window.location.assign('/create-event')
    }

    const handleDelete = (id) => () => {
        setIdToDelete(id);
    }

    let eventNames = events.map(event => {return {id: event.id, content: event.title}})

    let navToViewPage = (id) => () => {
        window.location.assign('/view-event?id=' + id)
    }

    return (
        <div style={{'paddingBottom': '100px'}}>
            <Header />
            <div  className={classes.root}>
                <Typography variant="h4" className='headingTyp'>
                    Event List
                </Typography>
                <div className="divContents">
                    <List>
                        {eventNames.map(el => (
                            <ViewableListItem delete={handleDelete} key={el.id} id={el.id} content={el.content} clicked={navToViewPage} />
                        ))}
                    </List>

                </div>
                <div className={classes.center}>
                    <Fab color="primary" aria-label="add" onClick={handleAdd}>
                        <AddIcon />
                    </Fab>

                </div>
            </div>
        </div>

    )
}

ReactDOM.render(<App />, document.getElementById('root'))