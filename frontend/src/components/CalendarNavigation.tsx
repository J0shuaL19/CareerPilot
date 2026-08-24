export type CalendarViewMode = 'MONTH' | 'WEEK'

interface CalendarNavigationProps {
  viewMode: CalendarViewMode
  periodLabel: string
  selectedDate: string
  onViewChange: (viewMode: CalendarViewMode) => void
  onDateChange: (date: string) => void
  onPrevious: () => void
  onToday: () => void
  onNext: () => void
}

export function CalendarNavigation({
  viewMode,
  periodLabel,
  selectedDate,
  onViewChange,
  onDateChange,
  onPrevious,
  onToday,
  onNext,
}: CalendarNavigationProps) {
  const periodName = viewMode === 'MONTH' ? 'month' : 'week'

  return (
    <div className="calendar-toolbar">
      <div className="calendar-toolbar__heading">
        <p>Schedule</p>
        <h2 id="calendar-period-heading">{periodLabel}</h2>
      </div>

      <div className="calendar-toolbar__controls">
        <div className="calendar-view-toggle" role="group" aria-label="Calendar view">
          <button
            type="button"
            aria-pressed={viewMode === 'MONTH'}
            onClick={() => onViewChange('MONTH')}
          >
            Month
          </button>
          <button
            type="button"
            aria-pressed={viewMode === 'WEEK'}
            onClick={() => onViewChange('WEEK')}
          >
            Week
          </button>
        </div>

        <label className="calendar-date-jump">
          <span>Jump to date</span>
          <input
            type="date"
            aria-label="Jump to date"
            value={selectedDate}
            onChange={(event) => onDateChange(event.target.value)}
          />
        </label>

        <div className="calendar-toolbar__actions">
          <button type="button" aria-label={`Previous ${periodName}`} onClick={onPrevious}>←</button>
          <button type="button" onClick={onToday}>Today</button>
          <button type="button" aria-label={`Next ${periodName}`} onClick={onNext}>→</button>
        </div>
      </div>
    </div>
  )
}
