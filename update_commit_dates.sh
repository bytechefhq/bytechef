#!/bin/bash

# Script to update author and commit dates for Git commits
# Sets dates to N days ago with random times from two periods: 5:00 AM - 8:00 AM or 10:00 PM - midnight
# Each commit randomly chooses one of these two time periods
#
# Usage: ./update_commit_dates.sh [number_of_commits] [days_ago] [time_period]
#
# Examples:
#   ./update_commit_dates.sh        # Uses defaults (3 commits, 1 day ago, evening period)
#   ./update_commit_dates.sh 30     # Changes the last 30 commits to 1 day ago, evening period
#   ./update_commit_dates.sh 10 7   # Changes the last 10 commits to 7 days ago, evening period
#   ./update_commit_dates.sh 10 7 morning   # Uses morning period (5h-8h)
#   ./update_commit_dates.sh 10 7 evening   # Uses evening period (22h-midnight)
#   ./update_commit_dates.sh 10 7 both      # Randomly chooses between both periods

# Default values
DEFAULT_NUM_COMMITS=3
DEFAULT_DAYS_AGO=1
DEFAULT_TIME_PERIOD="evening"

# Check for unstaged changes
if [[ -n $(git status --porcelain) ]]; then
    echo "Error: You have unstaged changes in your working directory."
    echo "Please commit or stash your changes before running this script."
    exit 1
fi

# Parse command line arguments
if [ $# -eq 0 ]; then
    NUM_COMMITS=$DEFAULT_NUM_COMMITS
    DAYS_AGO=$DEFAULT_DAYS_AGO
    TIME_PERIOD=$DEFAULT_TIME_PERIOD
    echo "No arguments provided. Using defaults: $NUM_COMMITS commits, $DAYS_AGO day(s) ago, $TIME_PERIOD period."
elif [ $# -eq 1 ]; then
    # Validate that the argument is a positive integer
    if [[ $1 =~ ^[0-9]+$ ]] && [ $1 -gt 0 ]; then
        NUM_COMMITS=$1
        DAYS_AGO=$DEFAULT_DAYS_AGO
        TIME_PERIOD=$DEFAULT_TIME_PERIOD
        echo "Will modify the last $NUM_COMMITS commits to $DAYS_AGO day(s) ago using $TIME_PERIOD period."
    else
        echo "Error: First argument must be a positive integer."
        echo "Usage: $0 [number_of_commits] [days_ago] [time_period]"
        exit 1
    fi
elif [ $# -eq 2 ]; then
    # Validate both arguments are positive integers
    if [[ $1 =~ ^[0-9]+$ ]] && [ $1 -gt 0 ] && [[ $2 =~ ^[0-9]+$ ]] && [ $2 -gt 0 ]; then
        NUM_COMMITS=$1
        DAYS_AGO=$2
        TIME_PERIOD=$DEFAULT_TIME_PERIOD
        echo "Will modify the last $NUM_COMMITS commits to $DAYS_AGO day(s) ago using $TIME_PERIOD period."
    else
        echo "Error: Both arguments must be positive integers."
        echo "Usage: $0 [number_of_commits] [days_ago] [time_period]"
        exit 1
    fi
elif [ $# -eq 3 ]; then
    # Validate first two arguments are positive integers and third is valid time period
    if [[ $1 =~ ^[0-9]+$ ]] && [ $1 -gt 0 ] && [[ $2 =~ ^[0-9]+$ ]] && [ $2 -gt 0 ]; then
        if [[ $3 == "morning" || $3 == "evening" || $3 == "both" ]]; then
            NUM_COMMITS=$1
            DAYS_AGO=$2
            TIME_PERIOD=$3
            echo "Will modify the last $NUM_COMMITS commits to $DAYS_AGO day(s) ago using $TIME_PERIOD period."
        else
            echo "Error: Third argument must be 'morning', 'evening', or 'both'."
            echo "Usage: $0 [number_of_commits] [days_ago] [time_period]"
            exit 1
        fi
    else
        echo "Error: First two arguments must be positive integers."
        echo "Usage: $0 [number_of_commits] [days_ago] [time_period]"
        exit 1
    fi
else
    echo "Error: Too many arguments."
    echo "Usage: $0 [number_of_commits] [days_ago] [time_period]"
    exit 1
fi

# Get the target date in YYYY-MM-DD format
# Check if we're on macOS or Linux
if [[ "$(uname)" == "Darwin" ]]; then
    # macOS date command
    TARGET_DATE=$(date -v-${DAYS_AGO}d "+%Y-%m-%d")
else
    # Linux date command
    TARGET_DATE=$(date -d "${DAYS_AGO} days ago" "+%Y-%m-%d")
fi

# Get current branch name
CURRENT_BRANCH=$(git symbolic-ref --short HEAD)
BACKUP_BRANCH="${CURRENT_BRANCH}_backup_$(date +%Y%m%d%H%M%S)"

echo "Setting commit dates to $DAYS_AGO day(s) ago ($TARGET_DATE) with random times:"
if [ "$TIME_PERIOD" == "morning" ]; then
    echo "  - Between 5:00 AM and 8:00 AM"
elif [ "$TIME_PERIOD" == "evening" ]; then
    echo "  - Between 10:00 PM and midnight"
else
    echo "  - Between 5:00 AM and 8:00 AM"
    echo "  - Between 10:00 PM and midnight"
    echo "Each commit will randomly choose one of these two time periods"
fi
echo "This will modify the last $NUM_COMMITS commits."
echo "WARNING: This operation rewrites Git history and is destructive."
echo "If you've already pushed these commits, you'll need to force push after this operation."
echo "This may cause problems for other collaborators who have pulled these commits."
echo "A backup branch named '$BACKUP_BRANCH' will be created."
echo "Press Ctrl+C to cancel or Enter to continue..."
read

# Create backup branch
echo "Creating backup branch '$BACKUP_BRANCH'..."
git branch $BACKUP_BRANCH

# Get the SHA of the commit before our target range
LAST_COMMIT=$(git rev-parse HEAD~$NUM_COMMITS)

# Get the list of commits to modify in reverse order (oldest first)
COMMITS=$(git rev-list --reverse $LAST_COMMIT..HEAD)

# Create a temporary file to store the filter script
FILTER_SCRIPT=$(mktemp)


# Start building the filter script
echo '#!/bin/bash' > $FILTER_SCRIPT
echo '' >> $FILTER_SCRIPT

# Process each commit
for COMMIT in $COMMITS; do
    # Generate random time based on selected time period
    if [ "$TIME_PERIOD" == "morning" ]; then
        # Morning period: 5:00 AM - 8:00 AM (hours 5-7)
        RANDOM_HOUR=$((RANDOM % 3 + 5))  # Hours 5, 6, 7
    elif [ "$TIME_PERIOD" == "evening" ]; then
        # Evening period: 10:00 PM - midnight (hours 22-23)
        RANDOM_HOUR=$((RANDOM % 2 + 22))  # Hours 22, 23
    else
        # Both periods: randomly choose between morning and evening
        if [ $((RANDOM % 2)) -eq 0 ]; then
            # Morning period: 5:00 AM - 8:00 AM
            RANDOM_HOUR=$((RANDOM % 3 + 5))  # Hours 5, 6, 7
        else
            # Evening period: 10:00 PM - midnight
            RANDOM_HOUR=$((RANDOM % 2 + 22))  # Hours 22, 23
        fi
    fi

    # Generate random minutes and seconds
    RANDOM_MINUTE=$((RANDOM % 60))
    RANDOM_SECOND=$((RANDOM % 60))

    # Format the random time as HH:MM:SS
    RANDOM_TIME=$(printf "%02d:%02d:%02d" $RANDOM_HOUR $RANDOM_MINUTE $RANDOM_SECOND)
    COMMIT_DATE="${TARGET_DATE}T${RANDOM_TIME}"

    # Add this commit to the filter script
    echo "if [ \$GIT_COMMIT = '$COMMIT' ]; then" >> $FILTER_SCRIPT
    echo "    export GIT_AUTHOR_DATE=\"$COMMIT_DATE\"" >> $FILTER_SCRIPT
    echo "    export GIT_COMMITTER_DATE=\"$COMMIT_DATE\"" >> $FILTER_SCRIPT
    echo "fi" >> $FILTER_SCRIPT
    echo "" >> $FILTER_SCRIPT

    echo "Will set commit $COMMIT to $COMMIT_DATE"
done

# Make the filter script executable
chmod +x $FILTER_SCRIPT

# Run git filter-branch with the filter script
echo "Rewriting commit dates..."
git filter-branch -f --env-filter "source $FILTER_SCRIPT" $LAST_COMMIT..HEAD

# Remove the temporary filter script
rm $FILTER_SCRIPT

echo -n "Done! The last $NUM_COMMITS commits now have their dates set to $DAYS_AGO day(s) ago with random times "
if [ "$TIME_PERIOD" == "morning" ]; then
    echo "between 5:00 AM - 8:00 AM."
elif [ "$TIME_PERIOD" == "evening" ]; then
    echo "between 10:00 PM - midnight."
else
    echo "from two periods: 5:00 AM - 8:00 AM or 10:00 PM - midnight."
fi
echo "A backup of the original state was created in branch: $BACKUP_BRANCH"
echo ""
echo "To push these changes to the remote repository, use: git push --force"
echo "To restore the original state, use: git reset --hard $BACKUP_BRANCH"
