const { parseArgs } = require('util');

function getArguments() {
    const args = [
        { name: "paId", mandatory: true, subcommand: [] },
        { name: "fileName", mandatory: false, subcommand: [] },
        { name: "envName", mandatory: true, subcommand: [] }
    ];

    const values = parseArgs({
        options: {
            paId: {
                type: "string", short: "p", default: undefined
            },
            fileName: {
                type: "string", short: "f", default: undefined
            },
            envName: {
                type: "string", short: "e", default: undefined
            }
        },
    });

    _checkingParameters(args, values);

    return values.values;
}

function _checkingParameters(args, values){
    const usage = "Usage: node index.js --envName <env-name> --fileName <file-name>] [--dryrun]"
    //CHECKING PARAMETER
    args.forEach(el => {
        if(el.mandatory && !values.values[el.name]){
            console.log("Param " + el.name + " is not defined")
            console.log(usage)
            process.exit(1)
        }
    })
    args.filter(el=> {
        return el.subcommand.length > 0
    }).forEach(el => {
        if(values.values[el.name]) {
            el.subcommand.forEach(val => {
                if (!values.values[val]) {
                    console.log("SubParam " + val + " is not defined")
                    console.log(usage)
                    process.exit(1)
                }
            })
        }
    })
}

module.exports = getArguments;
