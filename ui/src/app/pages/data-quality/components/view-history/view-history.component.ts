import { Component, Input, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { SnackbarService } from 'src/app/core/services/snackbar.service';
import { FormGroup, FormBuilder, Validators, FormControl } from '@angular/forms';
import {  ActivatedRoute, Router } from '@angular/router';
import { Observable, Subject } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';
import { Certificate } from 'src/app/models/certificate';
import { Quantumfacade } from 'src/app/state/quantum.facade';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ViewResultComponent } from '../view-result/view-result.component';
import { RuleCreationService } from '../../services/rule-creation.service';
import { ConfirmationComponent } from '../confirmation/confirmation.component';
import { NgbDateStruct, NgbCalendar } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-view-history',
  templateUrl: './view-history.component.html',
  styleUrls: ['./view-history.component.scss']
})
export class ViewHistoryComponent implements OnInit {
  projectId: number;
  userId: number;
  certificate$: Observable<Certificate>;
  certificateData: Certificate;
  private unsubscribe: Subject<void> = new Subject<void>();
  public loading:boolean;
  public runningList: any;

  public cancelBtn: boolean;
  public outputData: any;
  public startIndex: number = 0;
  public pageSize: number = 100;
  public endIndex: number = this.pageSize;
  public page = 1;

  anyCheckboxSelected: boolean = false;
  public ruleId: any = [];
  public selectedJobIds: number[] = [];
  public reRunStatus: boolean;
  public selectedReRunJobIds: number[] = [];
  selectedBusinessDate: string | null = null;

  public selectedLevels: { [ruleTypeName: string]: string } = {};
  public ruleTypeList: any;
  public filterPayload: any;
  public statusFilterList = [
    {
      id: 1, status: 'Completed',
      level: [
        { name: 'Matched' },
        { name: 'Mismatched' },
      ]
    },
    {id: 2, status: 'Failed'},
    {id: 3, status: 'Cancelled'},
    {id: 4, status: 'Inprogress'},
    {id: 5, status: 'Not Started'},
    {id: 6, status: 'InQueue'}
  ];
  public selectedStatusLevels: { [status: string]: string } = {};
  public selectedbusinessDate = 'Yesterday';
  public businessDate = [
    {name: 'Yesterday'},
    {name: 'Last Week'},
    {name: 'This Month'},
    {name: 'Custom'}
  ];
  public selectedFromBusinessDate: NgbDateStruct;
  public selectedToBusinessDate: NgbDateStruct;
  public isValidDateRange: boolean = false;
  public isValidFromDate: boolean = false;
  public areDatesSelected: boolean = false;
  // selectedBusinessDate: NgbDateStruct;
  maxDate: NgbDateStruct;
  public selectedDate: string;
  public resultArray: { ruleId: number, businessDate: string }[] = [];
  public selectedRules = [];

  constructor(
    // public modal: NgbActiveModal,
    private snakbar: SnackbarService,
    private fb: FormBuilder,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private quantumFacade: Quantumfacade,
    private modalService: NgbModal,
    private ruleCreationService: RuleCreationService,
    private calendar: NgbCalendar
  ){
    // this.projectId = parseInt(this.activatedRoute.parent.snapshot.paramMap.get('projectId'), 10);
    this.certificate$ = this.quantumFacade.certificate$;
    this.certificate$
      .pipe(takeUntil(this.unsubscribe))
      .subscribe(certificate => {
        this.certificateData = certificate;
        this.userId = +certificate.user_id;
      });
    this.maxDate = this.calendar.getToday();
  }

  ngOnInit(): void {
    this.projectId = +localStorage.getItem('project_id');
    this.getRulJobs();
    this.getRuleTypeList();
    this.onSelectBusinesDate(this.selectedbusinessDate);
  }

  public getRulJobs(): void {
    // this.loading = true;
    // const refreshPage = () => {
    //     this.autoRefresh();
    // };
    this.ruleCreationService.getRuleJobs(this.userId, this.projectId).subscribe((response) => {
      console.log("Rule data:", response);
      if (response?.code != 500) {
        this.runningList = response?.result;
        this.loading = false;
        // Check if there is at least one object with jobStatus "Inprocess"
        const hasInprocessJob = this.runningList.some(item => item.jobStatus === 'Inprocess' || item.jobStatus === 'Not Started');
        this.reRunStatus = this.runningList.some(item => item.jobStatus === 'Failed' || item.jobStatus === 'Cancelled');
        // If there is an in-process job, show the button; otherwise, hide it
        // Loop through the updated runningList and update selectedJobIds array
        // this.runningList.forEach(view => {
        //   const jobId = view.jobId;
        //   if (view.jobStatus === 'Not Started' || view.jobStatus === 'Inprocess') {
        //     if(this.selectedJobIds.length != 0) {
        //       // Check if jobId is already in the selectedJobIds array
        //       if (!this.selectedJobIds.includes(jobId)) {
        //         // If not, add it to the selectedJobIds array
        //         this.selectedJobIds.push(jobId);
        //       }
        //     }
            
        //   } else {
        //     // Check if jobId is in the selectedJobIds array
        //     const index = this.selectedJobIds.indexOf(jobId);
        //     if (index !== -1) {
        //       // If it is, remove it from the selectedJobIds array
        //       this.selectedJobIds.splice(index, 1);
        //     }
        //   }
        // });

        this.runningList.forEach(view => {
          const jobId = view.jobId;
          if (view.jobStatus === 'Not Started' || view.jobStatus === 'Inprocess') {
            const isSelected = this.selectedJobIds.includes(jobId);
            view.isChecked = isSelected; // Add an 'isChecked' property to each item
          } else {
            // Check if jobId is in the selectedJobIds array
            const index = this.selectedJobIds.indexOf(jobId);
            if (index !== -1) {
              // If it is, remove it from the selectedJobIds array
              this.selectedJobIds.splice(index, 1);
            }
          }
          
        });

        // if (hasInprocessJob) {
        //   setTimeout(refreshPage, 5000);
        //   this.cancelBtn = true;
        // } else {
        //   this.cancelBtn = false;
        // }
      } else {
        this.runningList = [];
        this.loading = false;

      }
      
    }, (error) => {
      this.loading = false;
    });
  }

  public getRuleTypeList(): void {
    this.loading = true;
    this.ruleCreationService.getAllRuletypes(this.projectId, true, true).subscribe((response) => {
      this.loading = false;
      this.ruleTypeList = response.result;
    }, (error) => {
      this.snakbar.open(error);
      this.loading = false;
    })
  }

  public autoRefresh(): void {

    this.getRulJobs();
  }

  public viewResult(r: any): void {
    const data = JSON.parse(r.jobOutput);

    if (Array.isArray(data)) {
      // It's a JSON array, you can loop through its elements
      for (const obj of data) {
        // Process each object in the array
        this.outputData = obj
        // You can add your conditional logic here for JSON array items
      }
    } else {
      // It's a JSON object
      this.outputData = data;
      // You can add your conditional logic for JSON objects here
    }

    const modalRef = this.modalService.open(ViewResultComponent, { size: 'md', windowClass: 'modal-size', scrollable: false });
    modalRef.componentInstance.userId = this.userId;
    modalRef.componentInstance.projectId = this.projectId;
    modalRef.componentInstance.output = this.outputData;
    modalRef.result.then((result) => {
      
    }, (result) => {
      
    });
  }

  public viewLog(v: any): void {
    localStorage.setItem('batchJobLog', v?.batchJobLog);
    this.router.navigate([]).then(() => { window.open(`/projects/${this.projectId}/data-quality/logs`, '_blank'); });
  }

  public cancelInprocess(): void {
    console.log(this.selectedJobIds);
    
    const modalRef = this.modalService.open(ConfirmationComponent, { size: 'md modal-dialog-centered', scrollable: false});
    modalRef.result.then((result) => {
      this.loading = true;
      // // Initialize an empty array to store jobIds
      // const jobIds: number[] = [];
  
      // // Iterate through the runningList to find objects with jobStatus "Inprocess"
      // for (const item of this.runningList) {
      //     if (item.jobStatus === 'Inprocess' || item.jobStatus === 'Not Started' ) {
      //         // Push the jobIds into the array
      //         jobIds.push(item.jobId);
      //     }
      // }
  
      const req = {
        jobIds : this.selectedJobIds
      };
  
      this.ruleCreationService.cancelRunningRule(this.userId, this.projectId, req).subscribe((res) => {
        this.loading = false;
        this.snakbar.open(res.message);
        this.getRulJobs();
      }, (error) => {
        this.loading = false;
      });
  
      // Now, 'jobIds' contains all the jobIds for objects with jobStatus "Inprocess"
      console.log(this.selectedJobIds); // You can use the 'jobIds' array as needed
    }, (error) => {
      this.snakbar.open(error);
    });
    
  }

  public reRunRuleFunction(): void {
    // console.log(this.selectedReRunJobIds);
    
    this.loading = true;
    const request = {
      rules: this.selectedRules
    }

    this.ruleCreationService.runRule(this.userId, this.projectId, request).subscribe((res) => {
      this.loading = false;
      this.selectedRules = [];
      this.selectedReRunJobIds = [];
      this.snakbar.open(res.message);
      this.getRulJobs();
    }, (error) => {
      this.loading = false;
      this.snakbar.open(error);
    });
    
    // const modalRef = this.modalService.open(ConfirmationComponent, { size: 'md modal-dialog-centered', scrollable: false});
    // modalRef.result.then((result) => {
    //   this.loading = true;
  
    //   const req = {
    //     jobIds : this.selectedJobIds
    //   };
  
    //   this.ruleCreationService.cancelRunningRule(this.userId, this.projectId, req).subscribe((res) => {
    //     this.loading = false;
    //     this.snakbar.open(res.message);
    //     this.getRulJobs();
    //   }, (error) => {
    //     this.loading = false;
    //   });
  
    //   // Now, 'jobIds' contains all the jobIds for objects with jobStatus "Inprocess"
    //   console.log(this.selectedJobIds); // You can use the 'jobIds' array as needed
    // }, (error) => {
    //   this.snakbar.open(error);
    // });
    
  }

  public refresh(): void {
    // call get function
    this.getRulJobs();
  }

  public back(): void {
    this.router.navigate([`projects/${this.projectId}/data-quality`]);
  }

  // Define a function to parse the jobOutput JSON string
  parseJobOutput(jobOutput: string) {
    try {
      const parsedOutput = JSON.parse(jobOutput);
      if (Array.isArray(parsedOutput)) {
        // Check if it's an array and contains an item with 'match' property
        const hasMatch = parsedOutput.some(item => item.match);
        return hasMatch ? 'Match' : 'MisMatch';
      } else if (parsedOutput.match !== undefined) {
        // Check if it's an object with 'match' property
        return parsedOutput.match ? 'Match' : 'MisMatch';
      } else {
        // If it doesn't have 'match' property, default to 'MisMatch'
        return 'MisMatch';
      }
    } catch (error) {
      // Handle the case where JSON parsing fails
      return 'MisMatch';
    }
  }

  public onPageChange(currentPage: number): void {
    this.startIndex = (currentPage - 1) * this.pageSize;
    this.endIndex = this.startIndex + this.pageSize;
  }

  selectRule(event: any, d: any): void {
    const jobId = d.jobId;

    if (event.target.checked) {
      // Checkbox is checked, add jobId to the selectedJobIds array
      this.selectedJobIds.push(jobId);
    } else {
      // Checkbox is unchecked, remove jobId from the selectedJobIds array
      const index = this.selectedJobIds.indexOf(jobId);
      if (index !== -1) {
        this.selectedJobIds.splice(index, 1);
      }
    }
    
  }

  selectReRule(event: any, d: any): void {
    // const jobId = d.ruleId;

    // // Clear the array before adding the new jobId
    // // this.selectedReRunJobIds = [];

    // if (event.target.checked) {
    //   // Checkbox is checked, add jobId to the selectedJobIds array
    //   this.selectedReRunJobIds.push(jobId);
    // } else {
    //   const i = this.selectedReRunJobIds.indexOf(jobId);
    //   if (i !== -1) {
    //     this.selectedReRunJobIds.splice(i, 1);
    //   }
    // }

    if (event.target.checked) {
      // If checkbox is checked, add the rule to the selectedRules array
      this.selectedRules.push({
        ruleId: d.ruleId,
        businessDate: d.businessDate
      });
    } else {
      // If checkbox is unchecked, remove the rule from the selectedRules array
      this.selectedRules = this.selectedRules.filter(selectedRule => selectedRule.ruleId !== d.ruleId);
    }

    // Log the selected rules (you can remove this in your final implementation)
    console.log('Selected Rules:', this.selectedRules);
    
  }

  isSelected(jobId: number): boolean {
    // Check if the jobId is in the selectedReRunJobIds array
    return this.selectedReRunJobIds.includes(jobId);
  }

  public reRun(d: any): void {
    this.loading = true;
    this.selectedReRunJobIds = [];
    this.selectedReRunJobIds.push(d.ruleId);
    this.selectedBusinessDate = d.businessDate;

    
    const request = {
      ruleIds: this.selectedReRunJobIds,
      businessDate: this.selectedBusinessDate
    }

    this.ruleCreationService.runRule(this.userId, this.projectId, request).subscribe((res) => {
      this.loading = false;
      this.selectedReRunJobIds = [];
      this.snakbar.open(res.message);
      this.getRulJobs();
    }, (error) => {
      this.loading = false;
      this.selectedReRunJobIds = [];
      this.snakbar.open(error);
    });
  }

  convertToUKTimeZone(timestamp: number) {
    if(timestamp) {
      const ukTimestamp = new Date(timestamp);

      // Check if Daylight Saving Time (DST) is in effect (typically from the last Sunday in March to the last Sunday in October)
      const today = new Date();
      const dstStart = new Date(today.getFullYear(), 2, 31 - ((5 * today.getFullYear() / 4 + 4) % 7), 1); // Last Sunday in March
      const dstEnd = new Date(today.getFullYear(), 9, 31 - ((5 * today.getFullYear() / 4 + 1) % 7), 1); // Last Sunday in October
      const isDST = today > dstStart && today < dstEnd;

      // Adjust for DST (1 hour ahead if DST is in effect)
      if (isDST) {
        ukTimestamp.setHours(ukTimestamp.getHours() + 1);
      }

      const day = ukTimestamp.getUTCDate();
      const month = ukTimestamp.getUTCMonth() + 1; // Months are zero-based, so add 1
      const year = ukTimestamp.getUTCFullYear() % 100; // Get the last two digits of the year
      const hours = ukTimestamp.getUTCHours();
      const minutes = ukTimestamp.getUTCMinutes();

      // Format date components with leading zeros if needed
      const formattedDate = `${day < 10 ? '0' : ''}${day}-${month < 10 ? '0' : ''}${month}-${year}`;
      const formattedTime = `${hours < 10 ? '0' : ''}${hours}:${minutes < 10 ? '0' : ''}${minutes}`;
      const amPm = hours < 12 ? 'AM' : 'PM';

      return `${formattedDate}, ${formattedTime} ${amPm}`;
    }
    
  }

  // filter by rule types 
  onCheckboxChange(item: any): void {
    if (item.checked) {
      this.selectedLevels[item.ruleTypeName] = 'All'; // Set default selected level to 'All'
    } else {
      delete this.selectedLevels[item.ruleTypeName];
    }
    
  }

  public onSelectLevelName(id: string): void {
    
  }

  // Filter by status 
  onCheckboxChangeStatus(item: any): void {
    if (item.checked) {
      this.selectedStatusLevels[item.status] = 'All'; // Set default selected level to 'All'
    } else {
      delete this.selectedStatusLevels[item.status];
    }
    
  }

  public onSelectStatusLevelName(id: string): void {
    
  }

  // Filter by business date 
  public onSelectBusinesDate(d: string): void {
    console.log(d);
    switch (d) {
      case 'Yesterday':
        // Logic to set yesterday's date in DD-MM-YYYY format
        this.setYesterdayDate();
        this.areDatesSelected = true;
        this.isValidDateRange = true;
        this.isValidFromDate = true;
        break;
      case 'Last Week':
        // Logic to set last Monday and Sunday dates
        this.setLastWeekDates();
        this.areDatesSelected = true;
        this.isValidDateRange = true;
        this.isValidFromDate = true;
        break;
      case 'This Month':
        // Logic to set start and end dates of the current month
        this.setThisMonthDates();
        this.areDatesSelected = true;
        this.isValidDateRange = true;
        this.isValidFromDate = true;
        break;
      case 'Custom':
        // Logic for handling custom date range
        // Implement your custom logic here
        this.selectedFromBusinessDate = null;
        this.selectedToBusinessDate = null;
        this.areDatesSelected = false;
        this.isValidDateRange = true;
        this.isValidFromDate = true;
        break;
      default:
        // Handle other cases if needed
        break;
    }
    
  }

  private setYesterdayDate(): void {
    // Logic to set yesterday's date in DD-MM-YYYY format
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    this.selectedDate = this.formatDate(yesterday);
    console.log("Date Yesterday", this.selectedDate);
    
  }

  private setLastWeekDates(): void {
    // Logic to set last Monday and Sunday dates
    const today = new Date();
    const lastMonday = new Date(today);
    lastMonday.setDate(today.getDate() - today.getDay() - 6); // Setting it to last Monday

    const lastSunday = new Date(today);
    lastSunday.setDate(today.getDate() - today.getDay()); // Setting it to last Sunday

    this.selectedDate = `${this.formatDate(lastMonday)} and ${this.formatDate(lastSunday)}`;
    console.log("Date Last week", this.selectedDate);
  }

  private setThisMonthDates(): void {
    // Logic to set start and end dates of the current month
    const today = new Date();
    const firstDayOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);
    const lastDayOfMonth = new Date(today.getFullYear(), today.getMonth() + 1, 0);

    this.selectedDate = `${this.formatDate(firstDayOfMonth)} - ${this.formatDate(lastDayOfMonth)}`;
    console.log("Date this month", this.selectedDate);
  }

  private formatDate(date: Date): string {
    // Helper function to format date in DD-MM-YYYY
    const day = ('0' + date.getDate()).slice(-2);
    const month = ('0' + (date.getMonth() + 1)).slice(-2);
    const year = date.getFullYear();
    return `${day}-${month}-${year}`;
  }

  // custom 
  onDateChange() {
    // Check if both 'From' and 'To' dates are selected
    this.areDatesSelected = this.checkIfDatesSelected();

    // If both dates are selected, check the 'From' date condition
    if (this.areDatesSelected) {
      this.isValidFromDate = this.checkFromDate();

      // Only check the date range condition if 'From' date condition is true
      if (this.isValidFromDate) {
        this.isValidDateRange = this.checkDateDifference();
      } else {
        this.isValidDateRange = true; // Reset the range validation if 'From' date is invalid
      }
    } else {
      // Reset both validations if dates are not selected
      this.isValidFromDate = true;
      this.isValidDateRange = true;
    }
  }

  checkIfDatesSelected(): boolean {
    return !!this.selectedFromBusinessDate && !!this.selectedToBusinessDate;
  }
  // Function to check the date difference
  checkDateDifference(): boolean {
    if (this.selectedFromBusinessDate && this.selectedToBusinessDate) {
      const fromDate = new Date(this.selectedFromBusinessDate.year, this.selectedFromBusinessDate.month - 1, this.selectedFromBusinessDate.day);
      const toDate = new Date(this.selectedToBusinessDate.year, this.selectedToBusinessDate.month - 1, this.selectedToBusinessDate.day);

      const differenceInMonths = Math.abs((toDate.getFullYear() - fromDate.getFullYear()) * 12 + toDate.getMonth() - fromDate.getMonth());

      return differenceInMonths <= 3;
    }

    return true; // Default to true if dates are not selected yet
  }

  checkFromDate(): boolean {
    if (this.selectedFromBusinessDate && this.selectedToBusinessDate) {
      const fromDate = new Date(this.selectedFromBusinessDate.year, this.selectedFromBusinessDate.month - 1, this.selectedFromBusinessDate.day);
      const toDate = new Date(this.selectedToBusinessDate.year, this.selectedToBusinessDate.month - 1, this.selectedToBusinessDate.day);

      return fromDate <= toDate;
    }

    return true; // Default to true if dates are not selected yet
  }

  getFormattedFromDate(): string {
    if (this.selectedFromBusinessDate) {
        const day = this.selectedFromBusinessDate.day.toString().padStart(2, '0');
        const month = this.selectedFromBusinessDate.month.toString().padStart(2, '0');
        const year = this.selectedFromBusinessDate.year;

        return `${day}-${month}-${year}`;
    }
    return '';
  }

  getFormattedToDate(): string {
    if (this.selectedToBusinessDate) {
        const day = this.selectedToBusinessDate.day.toString().padStart(2, '0');
        const month = this.selectedToBusinessDate.month.toString().padStart(2, '0');
        const year = this.selectedToBusinessDate.year;

        return `${day}-${month}-${year}`;
    }
    return '';
  }
}
