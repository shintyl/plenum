import React, { useEffect, useState } from 'react'
import ReactDOM from 'react-dom'
import Aggregate_calendar from "../components/aggregate-calendar";
import List_menu from '../components/list-menu'
import Header from "../components/header";
import { makeStyles, useTheme } from '@material-ui/core/styles';
import Card from '@material-ui/core/Card';
import CardContent from '@material-ui/core/CardContent';
import Typography from '@material-ui/core/Typography';
import { List } from '@material-ui/core';
import SaveIcon from '@material-ui/icons/Save';
import {TextField, Container, Button} from '@material-ui/core';

const useStyles = makeStyles((theme) => ({
    root: {
        display: 'flex',
    },
    details: {
        display: 'flex',
        flexDirection: 'column',
    },
    content: {
        flex: '1 0 auto',
    },
    title: {
        fontWeight: "bold",
        marginBottom: theme.spacing(4),
    },
    button: {
        marginLeft: theme.spacing(2),
        marginTop: "10px"
    }
}));

const App = () => {

    let getInitId = () => {
        let paramId = parseInt(new URLSearchParams(document.location.search.substring(1)).get("id"));
        if (isNaN(paramId)) {
            window.history.replaceState({ id: -1 }, '', '/view-event')
            return -1;
        }
        else {
            window.history.replaceState({ id: paramId }, '', '/view-event?id=' + paramId)
            return paramId;
        }
    }

    const [id, setId] = useState(getInitId)

    const [calendars, setCalendars] = useState([])

    console.log(calendars)

    const [file, setFile] = useState({})

    const [editable, setEditable] = useState(false)
    const [value, setValue] = React.useState(0);

    const [calOptions, setCalOptions] = useState([])

    const [selectedCal, setSelectedCal] = useState(null)

    const classes = useStyles();
    const theme = useTheme();

    useEffect(() => {
        fetch('/api/event', {
            method: 'GET',
            mode: 'cors'
        }
        ).then(res => {
            return res.json()
        }).then(data => {
            console.log(data)
            setCalendars([...data])
        })
    }, [])

    useEffect(() => {
        fetch('/api/calendar', {
            method: 'GET',
            mode: 'cors'
        }
        ).then(res => {
            return res.json()
        }).then(data => {
            console.log(data)
            setCalOptions(data)
        })
    }, [])

    useEffect(() => {
        if (id > 0) {
            fetch('/api/event?id=' + id, {
                method: 'GET',
                mode: 'cors'
            }
            ).then(res => {
                return res.json()
            }).then(data => {
                console.log(data)
                setFile(data)
            })
        }
        else {
            setFile({})
        }
    }, [id, editable])

    let updateActive = (id) => () => {
        window.history.pushState({ id: id }, '', '/view-event?id=' + id)
        setId(id)
    }

    let clearCalendarView = () => {
        window.history.pushState({ id: -1 }, '', '/view-event')
        setId(-1)
    }

    window.onpopstate = (e) => {
        setId(e.state.id)
    }

    function handleMenuChange(cal) {
        setSelectedCal(cal)
    }

    return (
        <div>
            <Header></Header>
            <Card className={classes.root}>
                <div className={classes.details}>
                    <CardContent className={classes.content}>
                        <Typography component="h4" variant="h4" className={classes.title}>
                            Event: {id}
                        </Typography>
                    </CardContent>
                    <Card>
                        <Aggregate_calendar />
                    </Card>
                </div>
            </Card>
            <List_menu options={calOptions.map(element => element.title)} onChange={handleMenuChange} ></List_menu>
            <Button
                variant="contained"
                color="secondary"
                className={classes.button}
                startIcon={<SaveIcon />}>
                Save Calendar to Event!
            </Button>
        </div>
    )
};

ReactDOM.render(<App />, document.getElementById('root'))